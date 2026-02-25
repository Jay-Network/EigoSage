import SwiftUI

struct AnnotationScreen: View {
    let capturedImage: CapturedImage
    var onBack: () -> Void

    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = AnnotationViewModel()

    @Environment(\.horizontalSizeClass) var hSizeClass
    @Environment(\.verticalSizeClass) var vSizeClass

    var isLandscape: Bool {
        vSizeClass == .compact
    }

    var body: some View {
        ZStack {
            // Layer 1: Full-screen interactive image viewer
            InteractiveImageView(
                capturedImage: viewModel.currentImage,
                interactionMode: viewModel.interactionMode,
                tappedWord: viewModel.tappedWord,
                onWordTapped: { viewModel.onWordTapped($0) },
                onWordLongPressed: { viewModel.onWordLongPressed($0) },
                onWordsSelected: { viewModel.selectWords($0) }
            )
            .ignoresSafeArea()

            // Layer 2: Top controls overlay
            VStack {
                HStack {
                    // Back button
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.title2.weight(.semibold))
                            .foregroundStyle(.white)
                            .padding(12)
                            .background(.ultraThinMaterial, in: Circle())
                    }

                    Spacer()

                    // OCR status badge
                    if viewModel.isCorrectingOcr {
                        HStack(spacing: 6) {
                            ProgressView()
                                .tint(.white)
                                .scaleEffect(0.7)
                            Text("Enhancing OCR...")
                                .font(EigoLensTheme.labelMedium)
                                .foregroundStyle(.white)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(.ultraThinMaterial, in: Capsule())
                    }

                    // Word count badge
                    Text("\(viewModel.currentImage.ocrResult.wordCount) words")
                        .font(EigoLensTheme.labelMedium)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(.ultraThinMaterial, in: Capsule())

                    // Mode toggle
                    Button(action: { viewModel.toggleInteractionMode() }) {
                        Image(systemName: viewModel.interactionMode == .tap ? "hand.tap.fill" : "selection.pin.in.out")
                            .font(.title2)
                            .foregroundStyle(.white)
                            .padding(12)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)

                Spacer()

                // AI FAB (bottom right when panel is not showing AI analysis)
                if shouldShowAiFab {
                    HStack {
                        Spacer()
                        Button(action: { viewModel.analyzeFullText() }) {
                            Image(systemName: "brain")
                                .font(.title2)
                                .foregroundStyle(.white)
                                .padding(16)
                                .background(EigoLensTheme.primary, in: Circle())
                                .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
                        }
                        .padding(.trailing, isLandscape && viewModel.panelState.isVisible ? viewModel.panelWidthForFab + 20 : 20)
                        .padding(.bottom, !isLandscape && viewModel.panelState.isVisible ? viewModel.panelHeightForFab + 20 : 20)
                    }
                }
            }

            // Layer 3: Draggable panel
            DraggablePanel(isVisible: viewModel.panelState.isVisible, isLandscape: isLandscape) {
                PanelContentView(
                    state: viewModel.panelState,
                    onDismiss: { viewModel.dismissPanel() },
                    onToggleBookmark: { viewModel.toggleBookmark() },
                    isBookmarked: viewModel.isCurrentWordBookmarked
                )
            }
        }
        .navigationBarHidden(true)
        .task {
            viewModel.configure(capturedImage: capturedImage, container: container)
        }
    }

    private var shouldShowAiFab: Bool {
        switch viewModel.panelState {
        case .aiLoading, .aiAnalysis: return false
        default: return viewModel.currentImage.ocrResult.wordCount > 0
        }
    }
}
