import Foundation
import UIKit

final class FeedbackRepository {
    private let baseURL: String
    private let anonKey: String

    init(baseURL: String, anonKey: String) {
        self.baseURL = baseURL
        self.anonKey = anonKey
    }

    var isConfigured: Bool {
        !baseURL.isEmpty && !anonKey.isEmpty
    }

    // MARK: - Submit Feedback

    func submitFeedback(
        email: String,
        category: FeedbackCategory,
        feedbackText: String
    ) async -> SubmitFeedbackResult {
        guard isConfigured else { return .error(message: "Backend not configured") }

        let body: [String: Any] = [
            "user_email": email,
            "app_id": "eigolens",
            "category": category.rawValue,
            "feedback_text": feedbackText,
            "device_info": deviceInfo()
        ]

        do {
            let (data, response) = try await postFunction("feedback-submit", body: body)
            guard let http = response as? HTTPURLResponse else {
                return .error(message: "Invalid response")
            }

            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]

            if http.statusCode == 429 {
                let errorObj = json?["error"] as? [String: Any]
                let msg = errorObj?["message"] as? String ?? "Rate limit exceeded"
                return .rateLimited(message: msg)
            }

            guard (200...299).contains(http.statusCode) else {
                let errorObj = json?["error"] as? [String: Any]
                let msg = errorObj?["message"] as? String ?? "Submit failed"
                return .error(message: msg)
            }

            guard let dataObj = json?["data"] as? [String: Any] else {
                return .error(message: "Invalid response format")
            }

            let feedbackId = (dataObj["feedback_id"] as? NSNumber)?.int64Value ?? 0
            let createdAt = dataObj["created_at"] as? String ?? ""
            return .success(feedbackId: feedbackId, createdAt: createdAt)
        } catch {
            return .error(message: error.localizedDescription)
        }
    }

    // MARK: - Get Feedback Updates

    func getFeedbackUpdates(email: String, sinceId: Int64? = nil) async -> [Feedback] {
        guard isConfigured else { return [] }

        var queryString = "user_email=\(email)&app_id=eigolens"
        if let sinceId {
            queryString += "&since_id=\(sinceId)"
        }

        do {
            let (data, response) = try await getFunction("feedback-get-updates?\(queryString)")
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                return []
            }

            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            guard let dataObj = json?["data"] as? [String: Any],
                  let feedbackList = dataObj["feedback"] as? [[String: Any]] else {
                return []
            }

            return feedbackList.compactMap { item in
                guard let id = (item["id"] as? NSNumber)?.int64Value else { return nil }
                return Feedback(
                    id: id,
                    category: FeedbackCategory(rawValue: item["category"] as? String ?? "other") ?? .other,
                    feedbackText: item["feedback_text"] as? String ?? "",
                    status: FeedbackStatus.from(item["status"] as? String ?? "pending"),
                    createdAt: item["created_at"] as? String ?? "",
                    updatedAt: item["updated_at"] as? String ?? "",
                    completionNote: item["completion_note"] as? String
                )
            }
        } catch {
            return []
        }
    }

    // MARK: - Private

    private func postFunction(_ functionName: String, body: [String: Any]) async throws -> (Data, URLResponse) {
        let url = URL(string: "\(baseURL)/functions/v1/\(functionName)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(anonKey, forHTTPHeaderField: "Authorization")
        request.setValue(anonKey, forHTTPHeaderField: "apikey")
        request.timeoutInterval = 15
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        return try await URLSession.shared.data(for: request)
    }

    private func getFunction(_ functionName: String) async throws -> (Data, URLResponse) {
        let url = URL(string: "\(baseURL)/functions/v1/\(functionName)")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(anonKey, forHTTPHeaderField: "Authorization")
        request.setValue(anonKey, forHTTPHeaderField: "apikey")
        request.timeoutInterval = 15
        return try await URLSession.shared.data(for: request)
    }

    private func deviceInfo() -> [String: String] {
        let device = UIDevice.current
        return [
            "os": "iPadOS",
            "osVersion": device.systemVersion,
            "device": device.model,
            "manufacturer": "Apple"
        ]
    }
}
