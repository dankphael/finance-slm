import SwiftUI

struct RootView: View {
    var body: some View {
        TabView {
            ModelsView()
                .tabItem { Label("Models", systemImage: "square.and.arrow.down") }
            InsightsView()
                .tabItem { Label("Insights", systemImage: "sparkles") }
        }
    }
}
