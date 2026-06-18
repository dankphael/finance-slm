import Foundation
import Shared

/// Observable app state backed by the shared KMP SDK.
///
/// NOTE: this is a scaffold. The Kotlin<->Swift interop type bridging
/// (e.g. casting sealed `DownloadState` subclasses, `[ModelInfo]` arrays)
/// should be validated in Xcode — exact exported names can vary.
@MainActor
final class AppState: ObservableObject {
    static let shared = AppState()

    let sdk = SharedSdk()

    @Published var catalog: [ModelInfo] = []
    @Published var downloaded: [ModelInfo] = []
    @Published var selectedModelPath: String?

    private init() {}

    func loadCatalog() {
        if let url = Bundle.main.url(forResource: "model_catalog", withExtension: "json"),
           let json = try? String(contentsOf: url, encoding: .utf8) {
            sdk.loadCatalog(json: json)
        }
        Task { await refresh() }
    }

    func refresh() async {
        do {
            catalog = try await sdk.catalogSnapshot()
            downloaded = try await sdk.downloadedSnapshot()
            selectedModelPath = try await sdk.selectedModel()?.downloadedPath
        } catch {
            print("AppState.refresh failed: \(error)")
        }
    }

    func isDownloaded(_ model: ModelInfo) -> Bool {
        downloaded.contains { $0.id == model.id }
    }

    func download(_ model: ModelInfo,
                  progress: @escaping (Float) -> Void,
                  done: @escaping () -> Void) {
        Task {
            do {
                let flow = try await sdk.downloadModel(modelId: model.id)
                _ = SwiftFlow(flow: flow).subscribe(
                    onEach: { state in
                        if let d = state as? DownloadStateDownloading {
                            progress(d.progress)
                        }
                    },
                    onComplete: {
                        Task { await self.refresh(); done() }
                    },
                    onError: { err in
                        print("download error: \(err)")
                        done()
                    }
                )
            } catch {
                print("download start failed: \(error)")
                done()
            }
        }
    }

    func select(_ model: ModelInfo) {
        Task {
            try? await sdk.selectModel(modelId: model.id)
            await refresh()
        }
    }
}
