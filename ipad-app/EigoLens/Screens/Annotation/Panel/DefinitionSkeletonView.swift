import SwiftUI

struct DefinitionSkeletonView: View {
    @State private var isAnimating = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Word header skeleton
            HStack {
                shimmer(width: 120, height: 32)
                Spacer()
                shimmer(width: 60, height: 20)
            }

            // POS chips skeleton
            HStack(spacing: 8) {
                shimmer(width: 50, height: 28)
                shimmer(width: 40, height: 28)
            }

            // Definition cards skeleton
            ForEach(0..<3, id: \.self) { _ in
                VStack(alignment: .leading, spacing: 8) {
                    shimmer(width: .infinity, height: 16)
                    shimmer(width: 200, height: 14)
                }
                .padding(12)
                .background(EigoLensTheme.surfaceVariant, in: RoundedRectangle(cornerRadius: EigoLensTheme.radiusM))
            }

            Spacer()
        }
        .padding()
        .onAppear { isAnimating = true }
    }

    private func shimmer(width: CGFloat, height: CGFloat) -> some View {
        RoundedRectangle(cornerRadius: 4)
            .fill(Color.gray.opacity(0.2))
            .frame(maxWidth: width == .infinity ? .infinity : width, minHeight: height, maxHeight: height)
            .overlay(
                RoundedRectangle(cornerRadius: 4)
                    .fill(
                        LinearGradient(
                            colors: [.clear, .white.opacity(0.3), .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .offset(x: isAnimating ? 200 : -200)
                    .animation(.linear(duration: 1.5).repeatForever(autoreverses: false), value: isAnimating)
            )
            .clipped()
    }
}
