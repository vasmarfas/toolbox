import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    @State private var barsHidden = false

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .statusBarHidden(barsHidden)
            .persistentSystemOverlays(barsHidden ? .hidden : .automatic)
            .onAppear {
                SystemBarsBridge.shared.onChange = { hidden in
                    barsHidden = hidden.boolValue
                }
            }
    }
}