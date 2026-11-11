# Aorus Boss Timer — Codemagic/GitHub Android Build

Projeto Android baseado no HTML do Aorus Boss Timer, preparado para gerar um APK automaticamente no Codemagic a partir de um repositório GitHub.

## O que o build gera

O workflow `android-debug-apk` gera um APK Android instalável e o disponibiliza em **Artifacts** no Codemagic.

O APK é um build **debug**, portanto já vem assinado para testes e pode ser instalado no celular. Para publicação na Google Play, será necessário criar/configurar uma assinatura de release.

## Como usar

1. Crie um repositório no GitHub, por exemplo `aorus-boss-timer`.
2. Extraia o conteúdo deste projeto e envie **todos os arquivos** para a raiz do repositório.
3. No Codemagic, conecte sua conta GitHub e instale/autorize o GitHub App do Codemagic para esse repositório.
4. Adicione o repositório como uma aplicação Android Native.
5. O Codemagic detectará `codemagic.yaml` na raiz.
6. Execute o workflow `android-debug-apk`.
7. Quando terminar, baixe o APK em **Artifacts**.

## Horários configurados

- Queen Chaos Cara: 13:15, 23:15
- King Devil Bird: 11:15, 21:15
- Kelvezu: 00:30, 06:30, 12:30, 18:30
- Valento: 04:30, 10:30, 16:30, 20:30
- Mokova: 02:30, 10:30, 14:30, 20:30
- Fury: 04:30, 08:30, 16:30, 22:30
- Shy: 00:30, 06:30, 12:30, 22:30
- Tulla: 09:00, 15:00, 22:00
- Babel: 02:30, 08:30, 14:30, 18:30
- Aorus God: sábado 13:00/20:30; domingo 09:30/19:30
- Flame Maiden: sábado 15:30; domingo 20:30
- Antrallos: terça e quinta 13:00/21:30

## Notificações

O aplicativo possui integração nativa para agendamento dos alertas de 15, 5, 1 minuto e spawn. A tela HTML continua sendo usada como interface dentro do WebView.

## Observação

O workflow baixa o Gradle 8.9 durante o build, então o repositório não precisa armazenar o Gradle inteiro. O Codemagic precisa ter acesso à internet durante a compilação.

---

# Versão iOS (iPhone)

A pasta `ios/` contém o app nativo para iPhone/iPad (Swift + WKWebView). Ele usa **o mesmo `index.html`** do Android (`app/src/main/assets/index.html`), então qualquer alteração na interface vale para os dois.

## O que foi portado

| Android | iOS |
|---|---|
| WebView + `AndroidNotifications` (JavaScript Interface) | WKWebView + mesma interface `AndroidNotifications` injetada via script (o HTML não precisou mudar) |
| AlarmManager + BossAlarmReceiver | Notificações locais (UNUserNotificationCenter) |
| Fuso America/Sao_Paulo, alertas 5 min / 1 min / nascimento | Igual |

## Diferenças inevitáveis do iOS

- **Limite de 64 notificações pendentes**: o app agenda as ~60 mais próximas e **renova a fila toda vez que é aberto**. Se ficar vários dias sem abrir, os alertas acabam.
- **Sem voz (TTS) em segundo plano**: o iOS não deixa o app falar com a tela bloqueada. O alerta chega como notificação com som padrão; a voz continua funcionando com o app aberto.
- Sem equivalente ao "alarme exato" do Android: as notificações respeitam Foco / Não Perturbe.

## Como gerar o .ipa (Codemagic)

Os workflows novos já estão no `codemagic.yaml`:

1. **`ios-unsigned-ipa`** – gera `AorusBossTimer-unsigned.ipa`, para instalar com **Sideloadly** ou **AltStore** usando um Apple ID gratuito (expira em 7 dias, precisa reinstalar).
2. **`ios-signed-testflight`** – gera IPA assinado e envia ao **TestFlight** (precisa de conta Apple Developer paga e integração App Store Connect no Codemagic).

Build local em um Mac: `brew install xcodegen && cd ios && xcodegen generate && open AorusBossTimer.xcodeproj`.
