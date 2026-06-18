import SwiftUI
import Shared

@main
struct FinanceSLMApp: App {
    init() {
        // Initialize Koin DI (shared + iOS platform modules) once at launch.
        KoinIosKt.doInitKoinIos()
        AppState.shared.loadCatalog()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(AppState.shared)
        }
    }
}
