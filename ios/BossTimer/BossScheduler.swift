import Foundation
import UserNotifications

/// Porte do BossScheduler.java para iOS.
/// Diferença importante: o iOS permite no máximo 64 notificações locais pendentes,
/// então agendamos as ~60 mais próximas (de todos os bosses/tipos) e renovamos a
/// fila sempre que o app é aberto.
enum BossScheduler {

    private static let defaults = UserDefaults.standard
    private static let brt = TimeZone(identifier: "America/Sao_Paulo")!
    private static let maxPending = 60
    private static let lookaheadDays = 15

    // 0 = 5 min, 1 = 1 min, 2 = nascimento
    private static let offsets: [TimeInterval] = [300, 60, 0]
    private static let typeKeys = ["5", "1", "spawn"]

    // MARK: - Estado salvo
    static func setBosses(json: String)     { defaults.set(json, forKey: "bosses") }
    static func setAlertTypes(json: String) { defaults.set(json, forKey: "alert_types") }
    static func setEnabled(_ v: Bool)       { defaults.set(v, forKey: "enabled") }
    static var isEnabled: Bool              { defaults.bool(forKey: "enabled") }

    private static var bosses: [[String: Any]] {
        guard let s = defaults.string(forKey: "bosses"),
              let d = s.data(using: .utf8),
              let a = try? JSONSerialization.jsonObject(with: d) as? [[String: Any]] else { return [] }
        return a
    }

    private static var enabledTypes: Set<String> {
        let s = defaults.string(forKey: "alert_types") ?? "[\"5\",\"1\",\"spawn\"]"
        guard let d = s.data(using: .utf8),
              let a = try? JSONSerialization.jsonObject(with: d) as? [Any] else { return ["5", "1", "spawn"] }
        return Set(a.compactMap { "\($0)" })
    }

    // MARK: - Permissão
    static func requestPermission(_ done: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                DispatchQueue.main.async { done(granted) }
            }
    }

    // MARK: - Agendamento
    static func rescheduleIfEnabled() {
        guard isEnabled else { return }
        scheduleAll()
    }

    static func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    static func scheduleAll() {
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()

        let types = enabledTypes
        let now = Date()
        var items: [(fire: Date, spawn: Date, typeIndex: Int, id: String, name: String)] = []

        for boss in bosses {
            let id = (boss["id"] as? String) ?? UUID().uuidString
            let name = (boss["name"] as? String) ?? "Boss"
            for spawn in upcomingSpawns(for: boss, from: now) {
                for t in 0..<3 where types.contains(typeKeys[t]) {
                    let fire = spawn.addingTimeInterval(-offsets[t])
                    if fire.timeIntervalSince(now) > 2 {
                        items.append((fire, spawn, t, id, name))
                    }
                }
            }
        }

        items.sort { $0.fire < $1.fire }

        for it in items.prefix(maxPending) {
            let upper = it.name.uppercased()
            let content = UNMutableNotificationContent()
            switch it.typeIndex {
            case 0:
                content.title = "🔴 BOSS EM 5 MINUTOS"
                content.body  = "O BOSS \(upper) NASCERÁ EM 5 MINUTOS"
            case 1:
                content.title = "🚨 BOSS EM 1 MINUTO"
                content.body  = "O BOSS \(upper) NASCERÁ EM 1 MINUTO"
            default:
                content.title = "⚔️ BOSS NASCEU!"
                content.body  = "O BOSS \(upper) NASCEU"
            }
            content.sound = .default
            if #available(iOS 15.0, *) { content.interruptionLevel = .active }

            let interval = max(1, it.fire.timeIntervalSinceNow)
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
            let ident = "\(it.id)-\(Int(it.spawn.timeIntervalSince1970))-\(it.typeIndex)"
            center.add(UNNotificationRequest(identifier: ident, content: content, trigger: trigger))
        }
    }

    // MARK: - Cálculo dos horários (fuso de Brasília, igual ao Android)
    private static func upcomingSpawns(for boss: [String: Any], from now: Date) -> [Date] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = brt
        let startOfToday = cal.startOfDay(for: now)
        var result: [Date] = []

        for d in 0..<lookaheadDays {
            guard let day = cal.date(byAdding: .day, value: d, to: startOfToday) else { continue }
            let weekday = cal.component(.weekday, from: day) - 1   // 0 = domingo
            for hm in times(for: boss, weekday: weekday) {
                let p = hm.split(separator: ":")
                guard p.count == 2, let h = Int(p[0]), let m = Int(p[1]),
                      let date = cal.date(bySettingHour: h, minute: m, second: 0, of: day) else { continue }
                if date.timeIntervalSince(now) > 1 { result.append(date) }
            }
        }
        return result
    }

    private static func times(for boss: [String: Any], weekday: Int) -> [String] {
        let times = (boss["times"] as? [String]) ?? []

        if (boss["scheduleType"] as? String) == "daily" { return times }

        let days = (boss["days"] as? [Any])?.compactMap { ($0 as? Int) ?? Int("\($0)") } ?? []
        guard days.contains(weekday) else { return [] }

        if let map = boss["dayTimeMap"] as? [String: Any] {
            return (map[String(weekday)] as? [String]) ?? []
        }
        return times
    }
}
