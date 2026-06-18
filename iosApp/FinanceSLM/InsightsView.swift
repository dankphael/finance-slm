import SwiftUI
import Shared

struct InsightsView: View {
    @EnvironmentObject var appState: AppState

    @State private var input: String = ""
    @State private var output: String = ""
    @State private var isGenerating = false

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Paste a transaction, balance, or statement and get an on-device insight. Everything stays on your phone.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                TextEditor(text: $input)
                    .frame(minHeight: 120)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3)))

                Button(action: generate) {
                    Text(isGenerating ? "Generating…" : "Generate Insight")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(isGenerating || input.isEmpty)

                if !output.isEmpty {
                    ScrollView {
                        Text(output).frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                Spacer()
            }
            .padding()
            .navigationTitle("Insights")
        }
    }

    private func generate() {
        guard let path = appState.selectedModelPath else {
            output = "Download and select a model on the Models tab first."
            return
        }
        isGenerating = true
        output = ""
        Task {
            do {
                let flow = try await appState.sdk.generateInsight(
                    text: input, modelPath: path, chatTemplate: "qwen"
                )
                _ = SwiftFlow(flow: flow).subscribe(
                    onEach: { token in
                        if let t = token as? String { output += t }
                    },
                    onComplete: { isGenerating = false },
                    onError: { _ in isGenerating = false }
                )
            } catch {
                output = "Error: \(error)"
                isGenerating = false
            }
        }
    }
}
