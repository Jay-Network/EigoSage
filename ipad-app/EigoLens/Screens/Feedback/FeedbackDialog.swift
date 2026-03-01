import SwiftUI

struct FeedbackDialog: View {
    @ObservedObject var viewModel: FeedbackViewModel

    var body: some View {
        ZStack {
            // Backdrop
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture { viewModel.closeDialog() }

            // Dialog card
            VStack(spacing: 0) {
                // Header
                HStack {
                    Image(systemName: "envelope.fill")
                        .foregroundStyle(EigoLensTheme.primary)
                    Text("Send Feedback")
                        .font(EigoLensTheme.titleLarge)
                        .foregroundStyle(EigoLensTheme.onSurface)
                    Spacer()
                    Button(action: { viewModel.closeDialog() }) {
                        Image(systemName: "xmark")
                            .foregroundStyle(EigoLensTheme.onSurfaceVariant)
                            .padding(8)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 8)

                Divider().background(Color.white.opacity(0.1))

                ScrollView {
                    VStack(spacing: 16) {
                        // Email field
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Email")
                                .font(EigoLensTheme.labelMedium)
                                .foregroundStyle(EigoLensTheme.onSurfaceVariant)
                            TextField("your@email.com", text: $viewModel.email)
                                .textContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .font(EigoLensTheme.bodyMedium)
                                .foregroundStyle(EigoLensTheme.onSurface)
                                .padding(12)
                                .background(Color.white.opacity(0.06))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(EigoLensTheme.outline, lineWidth: 1)
                                )
                        }

                        // Category selector
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Category")
                                .font(EigoLensTheme.labelMedium)
                                .foregroundStyle(EigoLensTheme.onSurfaceVariant)

                            LazyVGrid(columns: [
                                GridItem(.flexible()),
                                GridItem(.flexible()),
                                GridItem(.flexible())
                            ], spacing: 8) {
                                ForEach(FeedbackCategory.allCases, id: \.rawValue) { category in
                                    Button(action: { viewModel.selectedCategory = category }) {
                                        HStack(spacing: 4) {
                                            Image(systemName: category.icon)
                                                .font(.caption2)
                                            Text(category.label)
                                                .font(EigoLensTheme.labelSmall)
                                        }
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 8)
                                        .frame(maxWidth: .infinity)
                                        .background(
                                            viewModel.selectedCategory == category
                                                ? EigoLensTheme.primary.opacity(0.2)
                                                : Color.white.opacity(0.06)
                                        )
                                        .foregroundStyle(
                                            viewModel.selectedCategory == category
                                                ? EigoLensTheme.primary
                                                : EigoLensTheme.onSurfaceVariant
                                        )
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 8)
                                                .stroke(
                                                    viewModel.selectedCategory == category
                                                        ? EigoLensTheme.primary.opacity(0.5)
                                                        : EigoLensTheme.outline,
                                                    lineWidth: 1
                                                )
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }

                        // Feedback text
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text("Feedback")
                                    .font(EigoLensTheme.labelMedium)
                                    .foregroundStyle(EigoLensTheme.onSurfaceVariant)
                                Spacer()
                                Text("\(viewModel.feedbackText.count)/1000")
                                    .font(EigoLensTheme.labelSmall)
                                    .foregroundStyle(
                                        viewModel.feedbackText.count > 1000
                                            ? EigoLensTheme.error
                                            : EigoLensTheme.onSurfaceVariant
                                    )
                            }

                            TextEditor(text: $viewModel.feedbackText)
                                .font(EigoLensTheme.bodyMedium)
                                .foregroundStyle(EigoLensTheme.onSurface)
                                .scrollContentBackground(.hidden)
                                .frame(minHeight: 100, maxHeight: 150)
                                .padding(8)
                                .background(Color.white.opacity(0.06))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(EigoLensTheme.outline, lineWidth: 1)
                                )
                                .onChange(of: viewModel.feedbackText) { _, newValue in
                                    if newValue.count > 1000 {
                                        viewModel.feedbackText = String(newValue.prefix(1000))
                                    }
                                }
                        }

                        // Error / Success messages
                        if let error = viewModel.errorMessage {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                Text(error)
                            }
                            .font(EigoLensTheme.bodySmall)
                            .foregroundStyle(EigoLensTheme.error)
                        }

                        if let success = viewModel.successMessage {
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                Text(success)
                            }
                            .font(EigoLensTheme.bodySmall)
                            .foregroundStyle(EigoLensTheme.success)
                        }

                        // Submit button
                        Button(action: { viewModel.submit() }) {
                            HStack {
                                if viewModel.isSubmitting {
                                    ProgressView()
                                        .tint(.white)
                                        .scaleEffect(0.8)
                                } else {
                                    Image(systemName: "paperplane.fill")
                                }
                                Text(viewModel.isSubmitting ? "Sending..." : "Submit Feedback")
                            }
                            .font(EigoLensTheme.titleMedium)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(
                                viewModel.canSubmit
                                    ? EigoLensTheme.primary
                                    : EigoLensTheme.primary.opacity(0.3),
                                in: RoundedRectangle(cornerRadius: 12)
                            )
                        }
                        .disabled(!viewModel.canSubmit)
                        .buttonStyle(.plain)

                        // Feedback history
                        if !viewModel.feedbackHistory.isEmpty {
                            Divider().background(Color.white.opacity(0.1))

                            VStack(alignment: .leading, spacing: 8) {
                                Text("Your Feedback History")
                                    .font(EigoLensTheme.titleMedium)
                                    .foregroundStyle(EigoLensTheme.onSurface)

                                ForEach(viewModel.feedbackHistory) { item in
                                    FeedbackHistoryRow(feedback: item)
                                }
                            }
                        } else if viewModel.isLoadingHistory {
                            HStack {
                                ProgressView().tint(.white).scaleEffect(0.7)
                                Text("Loading history...")
                                    .font(EigoLensTheme.bodySmall)
                                    .foregroundStyle(EigoLensTheme.onSurfaceVariant)
                            }
                        }
                    }
                    .padding(20)
                }
            }
            .frame(maxWidth: 500)
            .frame(maxHeight: UIScreen.main.bounds.height * 0.85)
            .glassCard(cornerRadius: 16)
            .background(EigoLensTheme.background.opacity(0.95), in: RoundedRectangle(cornerRadius: 16))
            .padding(24)
        }
    }
}

// MARK: - History Row

private struct FeedbackHistoryRow: View {
    let feedback: Feedback

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(feedback.status.emoji)
                Text(feedback.status.label)
                    .font(EigoLensTheme.labelMedium)
                    .foregroundStyle(EigoLensTheme.onSurface)
                Spacer()
                Text(feedback.category.label)
                    .font(EigoLensTheme.labelSmall)
                    .foregroundStyle(EigoLensTheme.primary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(EigoLensTheme.primary.opacity(0.15), in: Capsule())
            }

            Text(feedback.feedbackText)
                .font(EigoLensTheme.bodySmall)
                .foregroundStyle(EigoLensTheme.onSurfaceVariant)
                .lineLimit(2)

            if let note = feedback.completionNote {
                Text(note)
                    .font(EigoLensTheme.labelSmall)
                    .italic()
                    .foregroundStyle(EigoLensTheme.success)
            }

            Text(formatDate(feedback.createdAt))
                .font(.system(size: 10))
                .foregroundStyle(EigoLensTheme.onSurfaceVariant.opacity(0.6))
        }
        .padding(10)
        .glassCard(cornerRadius: 8)
    }

    private func formatDate(_ iso: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: iso) {
            let display = DateFormatter()
            display.dateStyle = .medium
            display.timeStyle = .short
            return display.string(from: date)
        }
        return iso
    }
}
