import SwiftUI
import AVFoundation

struct CameraPreviewView: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewUIView {
        let view = PreviewUIView()
        view.session = session
        return view
    }

    func updateUIView(_ uiView: PreviewUIView, context: Context) {
        uiView.updateOrientation()
    }
}

final class PreviewUIView: UIView {
    var session: AVCaptureSession? {
        didSet {
            (layer as? AVCaptureVideoPreviewLayer)?.session = session
            updateOrientation()
        }
    }

    override class var layerClass: AnyClass {
        AVCaptureVideoPreviewLayer.self
    }

    private var previewLayer: AVCaptureVideoPreviewLayer? {
        layer as? AVCaptureVideoPreviewLayer
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.videoGravity = .resizeAspectFill
        updateOrientation()
    }

    func updateOrientation() {
        guard let connection = previewLayer?.connection else { return }

        // Use the window scene's interface orientation for reliable rotation
        if let windowScene = window?.windowScene {
            let orientation = windowScene.interfaceOrientation
            let angle: CGFloat
            switch orientation {
            case .landscapeLeft:
                angle = 0
            case .landscapeRight:
                angle = 180
            case .portraitUpsideDown:
                angle = 270
            default: // .portrait
                angle = 90
            }

            if connection.isVideoRotationAngleSupported(angle) {
                connection.videoRotationAngle = angle
            }
        }
    }
}
