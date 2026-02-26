import SwiftUI
import AVFoundation

struct CaptureScreen: View {
    var onNavigate: (NavRoute) -> Void
    var onCapture: (CapturedImage) -> Void

    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = CaptureViewModel()
    @State private var showGalleryPicker = false
    @Environment(\.verticalSizeClass) var vSizeClass

    var isLandscape: Bool {
        vSizeClass == .compact
    }

    var body: some View {
        ZStack {
            // Camera preview
            if viewModel.permissionGranted {
                CameraPreviewView(session: viewModel.session)
                    .ignoresSafeArea()
            } else {
                cameraPermissionView
            }

            // Controls overlay — adapts to landscape
            if isLandscape {
                landscapeControls
            } else {
                portraitControls
            }

            // Processing overlay
            if viewModel.isProcessing {
                Color.black.opacity(0.4)
                    .ignoresSafeArea()
                ProgressView("Processing...")
                    .tint(.white)
                    .foregroundStyle(.white)
                    .padding(24)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
            }
        }
        .navigationBarHidden(true)
        .task {
            viewModel.configure(ocrService: container.ocrService, onCapture: onCapture)
            await viewModel.requestPermission()
        }
        .sheet(isPresented: $showGalleryPicker) {
            GalleryPicker(onImageSelected: { image in
                viewModel.importFromGallery(image)
            })
        }
    }

    // MARK: - Landscape: controls on the right side

    private var landscapeControls: some View {
        HStack {
            // Top-left controls
            VStack {
                Button(action: { onNavigate(.history) }) {
                    controlIcon("clock.arrow.circlepath")
                }
                Button(action: { onNavigate(.settings) }) {
                    controlIcon("gearshape.fill")
                }
                Spacer()
            }
            .padding(.leading, 20)
            .padding(.top, 12)

            Spacer()

            // Right-side capture controls
            if viewModel.permissionGranted {
                VStack(spacing: 32) {
                    if viewModel.permissionGranted {
                        Button(action: { viewModel.toggleFlash() }) {
                            Image(systemName: viewModel.isFlashOn ? "bolt.fill" : "bolt.slash.fill")
                                .font(.title2)
                                .foregroundStyle(viewModel.isFlashOn ? .yellow : .white)
                                .padding(12)
                                .background(.ultraThinMaterial, in: Circle())
                        }
                    }

                    // Shutter
                    Button(action: { viewModel.capturePhoto() }) {
                        ZStack {
                            Circle().fill(.white).frame(width: 72, height: 72)
                            Circle().stroke(.white, lineWidth: 4).frame(width: 82, height: 82)
                        }
                    }
                    .disabled(viewModel.isProcessing)

                    // Gallery
                    Button(action: { showGalleryPicker = true }) {
                        controlIcon("photo.on.rectangle")
                    }
                }
                .padding(.trailing, 28)
            }
        }
    }

    // MARK: - Portrait: controls top/bottom

    private var portraitControls: some View {
        VStack {
            // Top bar
            HStack {
                Button(action: { onNavigate(.history) }) {
                    controlIcon("clock.arrow.circlepath")
                }

                Spacer()

                if viewModel.permissionGranted {
                    Button(action: { viewModel.toggleFlash() }) {
                        Image(systemName: viewModel.isFlashOn ? "bolt.fill" : "bolt.slash.fill")
                            .font(.title2)
                            .foregroundStyle(viewModel.isFlashOn ? .yellow : .white)
                            .padding(12)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                }

                Button(action: { onNavigate(.settings) }) {
                    controlIcon("gearshape.fill")
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)

            Spacer()

            // Bottom controls
            if viewModel.permissionGranted {
                HStack(spacing: 40) {
                    Button(action: { showGalleryPicker = true }) {
                        controlIcon("photo.on.rectangle")
                            .frame(width: 56, height: 56)
                    }

                    Button(action: { viewModel.capturePhoto() }) {
                        ZStack {
                            Circle().fill(.white).frame(width: 72, height: 72)
                            Circle().stroke(.white, lineWidth: 4).frame(width: 82, height: 82)
                        }
                    }
                    .disabled(viewModel.isProcessing)

                    Color.clear.frame(width: 56, height: 56)
                }
                .padding(.bottom, 32)
            }
        }
    }

    private func controlIcon(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.title2)
            .foregroundStyle(.white)
            .padding(12)
            .background(.ultraThinMaterial, in: Circle())
    }

    private var cameraPermissionView: some View {
        VStack(spacing: 20) {
            Image(systemName: "camera.fill")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("Camera Access Required")
                .font(EigoLensTheme.headlineMedium)
            Text("EigoLens needs camera access to capture text for analysis.")
                .font(EigoLensTheme.bodyMedium)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 48)
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(EigoLensTheme.primary)
        }
    }
}
