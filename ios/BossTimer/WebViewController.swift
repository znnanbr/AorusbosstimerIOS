import UIKit
import WebKit

final class WebViewController: UIViewController, WKScriptMessageHandler, WKNavigationDelegate, WKUIDelegate {

    private var webView: WKWebView!
    private let bg = UIColor(red: 13/255, green: 10/255, blue: 7/255, alpha: 1)

    // Recria a interface `window.AndroidNotifications` que o index.html já usa
    private let bridgeJS = """
    (function () {
      function post(a, p) {
        try { window.webkit.messageHandlers.bossBridge.postMessage({ action: a, payload: p || null }); } catch (e) {}
      }
      window.AndroidNotifications = {
        requestPermission:      function ()  { post('requestPermission'); },
        enableNotifications:    function (j) { post('enable', j); },
        disableNotifications:   function ()  { post('disable'); },
        setAlertTypes:          function (j) { post('setAlertTypes', j); },
        schedule:               function (j) { post('enable', j); },
        cancel:                 function ()  { post('disable'); }
      };
    })();
    """

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = bg

        let config = WKWebViewConfiguration()
        config.mediaTypesRequiringUserActionForPlayback = []
        config.allowsInlineMediaPlayback = true

        let ucc = WKUserContentController()
        ucc.addUserScript(WKUserScript(source: bridgeJS,
                                       injectionTime: .atDocumentStart,
                                       forMainFrameOnly: true))
        ucc.add(self, name: "bossBridge")
        config.userContentController = ucc

        webView = WKWebView(frame: .zero, configuration: config)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.isOpaque = false
        webView.backgroundColor = bg
        webView.scrollView.backgroundColor = bg
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.navigationDelegate = self
        webView.uiDelegate = self
        view.addSubview(webView)

        // Respeita notch / Dynamic Island / barra inferior
        let g = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: g.topAnchor),
            webView.bottomAnchor.constraint(equalTo: g.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: g.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: g.trailingAnchor)
        ])

        if let url = Bundle.main.url(forResource: "index", withExtension: "html") {
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }
    }

    override var preferredStatusBarStyle: UIStatusBarStyle { .lightContent }

    // MARK: - Ponte JS -> nativo
    func userContentController(_ userContentController: WKUserContentController,
                               didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any],
              let action = body["action"] as? String else { return }
        let payload = body["payload"] as? String

        switch action {
        case "requestPermission":
            BossScheduler.requestPermission { _ in }
        case "enable":
            BossScheduler.setBosses(json: payload ?? "[]")
            BossScheduler.setEnabled(true)
            BossScheduler.requestPermission { granted in
                if granted { BossScheduler.rescheduleIfEnabled() }
            }
        case "disable":
            BossScheduler.setEnabled(false)
            BossScheduler.cancelAll()
        case "setAlertTypes":
            BossScheduler.setAlertTypes(json: payload ?? "[\"5\",\"1\",\"spawn\"]")
            BossScheduler.rescheduleIfEnabled()
        default:
            break
        }
    }

    // Links externos abrem no Safari
    func webView(_ webView: WKWebView,
                 decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if navigationAction.navigationType == .linkActivated,
           let url = navigationAction.request.url,
           url.scheme == "http" || url.scheme == "https" {
            UIApplication.shared.open(url)
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }
}
