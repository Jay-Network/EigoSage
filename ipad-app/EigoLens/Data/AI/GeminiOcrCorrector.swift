import Foundation
import UIKit

final class GeminiOcrCorrector {
    private let apiKey: String
    private static let model = "gemini-2.5-flash"
    private static let baseURL = "https://generativelanguage.googleapis.com/v1beta/models"

    var isAvailable: Bool { !apiKey.isEmpty }

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    // MARK: - Extract Text (Vision OCR Correction)

    func extractText(from image: UIImage) async -> Result<[String], Error> {
        guard isAvailable else {
            return .failure(AiError.notConfigured("Gemini"))
        }

        guard let base64Image = imageToBase64(image) else {
            return .failure(AiError.parseError("Failed to encode image"))
        }

        let body: [String: Any] = [
            "contents": [[
                "parts": [
                    ["text": Self.extractPrompt],
                    ["inline_data": [
                        "mime_type": "image/jpeg",
                        "data": base64Image
                    ]]
                ]
            ]],
            "generationConfig": [
                "maxOutputTokens": 2048,
                "temperature": 0.1
            ]
        ]

        let url = URL(string: "\(Self.baseURL)/\(Self.model):generateContent?key=\(apiKey)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 30

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                return .failure(AiError.apiError("Gemini Vision", (response as? HTTPURLResponse)?.statusCode ?? 0))
            }

            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            let content = (json?["candidates"] as? [[String: Any]])?
                .first?["content"] as? [String: Any]
            guard let text = (content?["parts"] as? [[String: Any]])?
                .first?["text"] as? String else {
                return .failure(AiError.emptyResponse)
            }

            let lines = text.components(separatedBy: .newlines).filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            return .success(lines)
        } catch {
            return .failure(error)
        }
    }

    // MARK: - Contextual Insight

    func getContextualInsight(word: String, surroundingText: String) async -> Result<ContextualInsight, Error> {
        guard isAvailable else {
            return .failure(AiError.notConfigured("Gemini"))
        }

        let prompt = """
        Word: "\(word)"
        Context: "\(surroundingText)"

        For the word "\(word)" as used in the context above, provide:
        1. MEANING: The specific meaning in this context (one clear sentence)
        2. POS: The part of speech as used here (noun/verb/adj/adv/prep/conj)
        3. NOTE: A brief usage or grammar note relevant to this context (optional, one sentence)

        Format your response EXACTLY as:
        MEANING: <meaning>
        POS: <part of speech>
        NOTE: <note>
        """

        let body: [String: Any] = [
            "contents": [[
                "parts": [["text": prompt]]
            ]],
            "generationConfig": [
                "maxOutputTokens": 256,
                "temperature": 0.1
            ]
        ]

        let url = URL(string: "\(Self.baseURL)/\(Self.model):generateContent?key=\(apiKey)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 15

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                return .failure(AiError.apiError("Gemini", (response as? HTTPURLResponse)?.statusCode ?? 0))
            }

            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            let content = (json?["candidates"] as? [[String: Any]])?
                .first?["content"] as? [String: Any]
            guard let text = (content?["parts"] as? [[String: Any]])?
                .first?["text"] as? String else {
                return .failure(AiError.emptyResponse)
            }

            return .success(parseInsight(text))
        } catch {
            return .failure(error)
        }
    }

    // MARK: - Private

    private func parseInsight(_ text: String) -> ContextualInsight {
        var meaning = ""
        var pos = ""
        var note: String?

        for line in text.components(separatedBy: .newlines) {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            if trimmed.lowercased().hasPrefix("meaning:") {
                meaning = String(trimmed.dropFirst("meaning:".count)).trimmingCharacters(in: .whitespaces)
            } else if trimmed.lowercased().hasPrefix("pos:") {
                pos = String(trimmed.dropFirst("pos:".count)).trimmingCharacters(in: .whitespaces)
            } else if trimmed.lowercased().hasPrefix("note:") {
                let n = String(trimmed.dropFirst("note:".count)).trimmingCharacters(in: .whitespaces)
                if !n.isEmpty { note = n }
            }
        }

        return ContextualInsight(
            meaning: meaning.isEmpty ? (text.components(separatedBy: .newlines).first?.trimmingCharacters(in: .whitespaces) ?? text) : meaning,
            partOfSpeech: pos,
            note: note
        )
    }

    private func imageToBase64(_ image: UIImage) -> String? {
        guard let data = image.jpegData(compressionQuality: 0.85) else { return nil }
        return data.base64EncodedString()
    }

    // MARK: - Prompt

    private static let extractPrompt = """
    Extract ALL text from this image with perfect accuracy.
    Rules:
    - One line of text per output line
    - Preserve the reading order (top to bottom, left to right)
    - Use surrounding context to resolve ambiguous characters (e.g. "rn" vs "m", "l" vs "1", "O" vs "0")
    - If a word looks misspelled but context suggests the correct spelling, output the CORRECT spelling
    - Preserve original punctuation and capitalization
    - Do NOT skip any text, even small or partial words
    - Do NOT add any commentary, headings, labels, or markdown formatting
    - Output ONLY the extracted text, nothing else
    """
}
