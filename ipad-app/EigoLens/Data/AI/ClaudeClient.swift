import Foundation

final class ClaudeClient: AiProviderProtocol {
    let name = "Claude"
    private let apiKey: String
    private let model: String

    static let defaultModel = "claude-haiku-4-5-20251001"
    private static let apiURL = URL(string: "https://api.anthropic.com/v1/messages")!

    var isAvailable: Bool { !apiKey.isEmpty }

    init(apiKey: String, model: String = ClaudeClient.defaultModel) {
        self.apiKey = apiKey
        self.model = model
    }

    func analyze(context: AnalysisContext) async throws -> AiResponse {
        guard isAvailable else {
            throw AiError.notConfigured("Claude")
        }

        let prompt = AiPrompts.build(for: context)
        let body: [String: Any] = [
            "model": model,
            "max_tokens": 1024,
            "system": AiPrompts.systemPrompt,
            "messages": [["role": "user", "content": prompt]]
        ]

        var request = URLRequest(url: Self.apiURL)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        request.setValue("2023-06-01", forHTTPHeaderField: "anthropic-version")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 30

        let start = Date()
        let (data, response) = try await URLSession.shared.data(for: request)
        let elapsed = Int(Date().timeIntervalSince(start) * 1000)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AiError.apiError("Claude", 0)
        }
        guard httpResponse.statusCode == 200 else {
            throw AiError.apiError("Claude", httpResponse.statusCode)
        }

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let content = (json?["content"] as? [[String: Any]])?.first?["text"] as? String else {
            throw AiError.emptyResponse
        }

        let usage = json?["usage"] as? [String: Any]
        let inputTokens = usage?["input_tokens"] as? Int ?? 0
        let outputTokens = usage?["output_tokens"] as? Int ?? 0

        return AiResponse(
            content: content,
            provider: "Claude",
            model: model,
            tokensUsed: inputTokens + outputTokens,
            processingTimeMs: elapsed
        )
    }
}
