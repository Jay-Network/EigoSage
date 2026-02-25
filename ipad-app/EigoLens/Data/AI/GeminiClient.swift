import Foundation

final class GeminiClient: AiProviderProtocol {
    let name = "Gemini"
    private let apiKey: String
    private let model: String

    static let defaultModel = "gemini-2.5-flash"
    private static let baseURL = "https://generativelanguage.googleapis.com/v1beta/models"

    var isAvailable: Bool { !apiKey.isEmpty }

    init(apiKey: String, model: String = GeminiClient.defaultModel) {
        self.apiKey = apiKey
        self.model = model
    }

    func analyze(context: AnalysisContext) async throws -> AiResponse {
        guard isAvailable else {
            throw AiError.notConfigured("Gemini")
        }

        let prompt = AiPrompts.build(for: context)

        let body: [String: Any] = [
            "contents": [[
                "parts": [["text": prompt]]
            ]],
            "generationConfig": [
                "maxOutputTokens": 1024,
                "temperature": 0.3
            ],
            "systemInstruction": [
                "parts": [["text": AiPrompts.systemPrompt]]
            ]
        ]

        let url = URL(string: "\(Self.baseURL)/\(model):generateContent?key=\(apiKey)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 30

        let start = Date()
        let (data, response) = try await URLSession.shared.data(for: request)
        let elapsed = Int(Date().timeIntervalSince(start) * 1000)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw AiError.apiError("Gemini", (response as? HTTPURLResponse)?.statusCode ?? 0)
        }

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let content = (json?["candidates"] as? [[String: Any]])?
            .first?["content"] as? [String: Any]
        let text = (content?["parts"] as? [[String: Any]])?
            .first?["text"] as? String

        guard let text, !text.isEmpty else {
            throw AiError.emptyResponse
        }

        let usageMetadata = json?["usageMetadata"] as? [String: Any]
        let totalTokens = usageMetadata?["totalTokenCount"] as? Int

        return AiResponse(
            content: text,
            provider: "Gemini",
            model: model,
            tokensUsed: totalTokens,
            processingTimeMs: elapsed
        )
    }
}
