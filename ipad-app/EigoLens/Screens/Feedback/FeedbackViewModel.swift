import SwiftUI

@MainActor
final class FeedbackViewModel: ObservableObject {
    @Published var isDialogOpen = false
    @Published var email = ""
    @Published var feedbackText = ""
    @Published var selectedCategory: FeedbackCategory = .bug
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var feedbackHistory: [Feedback] = []
    @Published var isLoadingHistory = false

    private var repository: FeedbackRepository?
    private var pollTask: Task<Void, Never>?

    func configure(repository: FeedbackRepository) {
        self.repository = repository
        // Restore saved email
        if let saved = UserDefaults.standard.string(forKey: "feedback_email"), !saved.isEmpty {
            email = saved
        }
    }

    func openDialog() {
        isDialogOpen = true
        errorMessage = nil
        successMessage = nil
        loadHistory()
        startPolling()
    }

    func closeDialog() {
        isDialogOpen = false
        stopPolling()
    }

    var canSubmit: Bool {
        email.contains("@") && feedbackText.count >= 10 && !isSubmitting
    }

    func submit() {
        guard let repository, canSubmit else { return }

        isSubmitting = true
        errorMessage = nil
        successMessage = nil

        // Save email for next use
        UserDefaults.standard.set(email, forKey: "feedback_email")

        Task {
            let result = await repository.submitFeedback(
                email: email,
                category: selectedCategory,
                feedbackText: feedbackText
            )

            isSubmitting = false

            switch result {
            case .success:
                successMessage = "Feedback submitted! We'll review it soon."
                feedbackText = ""
                loadHistory()
            case .rateLimited(let message):
                errorMessage = message
            case .error(let message):
                errorMessage = message
            }
        }
    }

    private func loadHistory() {
        guard let repository, email.contains("@") else { return }
        isLoadingHistory = true

        Task {
            let history = await repository.getFeedbackUpdates(email: email)
            feedbackHistory = history.sorted { $0.id > $1.id }
            isLoadingHistory = false
        }
    }

    private func startPolling() {
        stopPolling()
        pollTask = Task {
            while !Task.isCancelled && isDialogOpen {
                try? await Task.sleep(for: .seconds(15))
                guard !Task.isCancelled && isDialogOpen else { break }
                loadHistory()
            }
        }
    }

    private func stopPolling() {
        pollTask?.cancel()
        pollTask = nil
    }
}
