import SwiftUI

struct DraggablePanel<Content: View>: View {
    let isVisible: Bool
    let isLandscape: Bool
    @ViewBuilder let content: () -> Content

    @State private var panelFraction: CGFloat = 0.45

    var body: some View {
        GeometryReader { geo in
            if isLandscape {
                landscapePanel(geo: geo)
            } else {
                portraitPanel(geo: geo)
            }
        }
        .allowsHitTesting(isVisible)
    }

    // MARK: - Landscape (Right-Side Panel)

    @ViewBuilder
    private func landscapePanel(geo: GeometryProxy) -> some View {
        let minWidth: CGFloat = 200
        let maxWidth = geo.size.width * 0.65
        let panelWidth = (geo.size.width * panelFraction).clamped(to: minWidth...maxWidth)

        HStack(spacing: 0) {
            Spacer()

            if isVisible {
                HStack(spacing: 0) {
                    // Drag handle (left edge of panel)
                    Rectangle()
                        .fill(Color.clear)
                        .frame(width: 20)
                        .contentShape(Rectangle())
                        .overlay(
                            RoundedRectangle(cornerRadius: 2)
                                .fill(Color.secondary.opacity(0.4))
                                .frame(width: 4, height: 44)
                        )
                        .gesture(
                            DragGesture()
                                .onChanged { value in
                                    let delta = -value.translation.width
                                    let newWidth = panelWidth + delta
                                    panelFraction = (newWidth / geo.size.width)
                                        .clamped(to: (minWidth / geo.size.width)...0.65)
                                }
                        )

                    // Panel content
                    content()
                        .frame(width: panelWidth - 20)
                        .background(.regularMaterial)
                }
                .frame(width: panelWidth, height: geo.size.height)
                .clipShape(
                    UnevenRoundedRectangle(
                        topLeadingRadius: 20,
                        bottomLeadingRadius: 20,
                        bottomTrailingRadius: 0,
                        topTrailingRadius: 0
                    )
                )
                .shadow(color: .black.opacity(0.2), radius: 10, x: -5)
                .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
        .animation(.spring(duration: 0.35, bounce: 0.15), value: isVisible)
    }

    // MARK: - Portrait (Bottom Sheet)

    @ViewBuilder
    private func portraitPanel(geo: GeometryProxy) -> some View {
        let minHeight: CGFloat = 200
        let maxHeight = geo.size.height * 0.85
        let panelHeight = (geo.size.height * panelFraction).clamped(to: minHeight...maxHeight)

        VStack(spacing: 0) {
            Spacer()

            if isVisible {
                VStack(spacing: 0) {
                    // Drag handle (top of panel)
                    HStack {
                        Spacer()
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Color.secondary.opacity(0.4))
                            .frame(width: 44, height: 4)
                        Spacer()
                    }
                    .frame(height: 20)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture()
                            .onChanged { value in
                                let delta = -value.translation.height
                                let newHeight = panelHeight + delta
                                panelFraction = (newHeight / geo.size.height)
                                    .clamped(to: (minHeight / geo.size.height)...0.85)
                            }
                    )

                    // Panel content
                    content()
                        .frame(height: panelHeight - 20)
                }
                .frame(width: geo.size.width, height: panelHeight)
                .background(.regularMaterial)
                .clipShape(
                    UnevenRoundedRectangle(
                        topLeadingRadius: 20,
                        bottomLeadingRadius: 0,
                        bottomTrailingRadius: 0,
                        topTrailingRadius: 20
                    )
                )
                .shadow(color: .black.opacity(0.2), radius: 10, y: -5)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(duration: 0.35, bounce: 0.15), value: isVisible)
    }
}
