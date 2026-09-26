import SwiftUI
import Shared
#if canImport(FirebaseCore)
import FirebaseCore
#endif
#if canImport(FirebaseAnalytics)
import FirebaseAnalytics
#endif

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        #if canImport(FirebaseCore)
        FirebaseApp.configure()
        #endif
        #if canImport(FirebaseAnalytics)
        AnalyticsBridge.shared.logEvent = { name, params in FirebaseAnalytics.Analytics.logEvent(name, parameters: params) }
        AnalyticsBridge.shared.setUserProperty = { name, value in FirebaseAnalytics.Analytics.setUserProperty(value, forName: name) }
        #endif
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
