import SwiftUI
import Shared

struct ModelsView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        NavigationView {
            List(appState.catalog, id: \.id) { model in
                ModelRow(model: model)
            }
            .navigationTitle("Models")
            .refreshable { await appState.refresh() }
        }
    }
}

private struct ModelRow: View {
    @EnvironmentObject var appState: AppState
    let model: ModelInfo

    @State private var progress: Float = 0
    @State private var downloading = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(model.name).font(.headline)
                Spacer()
                if appState.isDownloaded(model) {
                    Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                }
            }
            // Kotlin's `description` collides with NSObject.description, so
            // Kotlin/Native exports it as `description_`.
            Text(model.description_)
                .font(.subheadline)
                .foregroundColor(.secondary)

            if appState.isDownloaded(model) {
                Button("Use this model") { appState.select(model) }
                    .buttonStyle(.bordered)
            } else if downloading {
                ProgressView(value: Double(progress))
                Text("Downloading… \(Int(progress * 100))%")
                    .font(.caption).foregroundColor(.secondary)
            } else {
                Button("Download") {
                    downloading = true
                    appState.download(model, progress: { p in
                        progress = p
                    }, done: {
                        downloading = false
                    })
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding(.vertical, 4)
    }
}
