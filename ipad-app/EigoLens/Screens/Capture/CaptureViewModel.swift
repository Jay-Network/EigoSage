import SwiftUI
import AVFoundation
import UIKit

@MainActor
final class CaptureViewModel: NSObject, ObservableObject {
    @Published var permissionGranted = false
    @Published var isProcessing = false
    @Published var isFlashOn = false

    let session = AVCaptureSession()
    private var photoOutput = AVCapturePhotoOutput()
    private var ocrService: OCRService?
    private var onCapture: ((CapturedImage) -> Void)?
    private var photoContinuation: CheckedContinuation<UIImage, Error>?

    func configure(ocrService: OCRService, onCapture: @escaping (CapturedImage) -> Void) {
        self.ocrService = ocrService
        self.onCapture = onCapture
    }

    func requestPermission() async {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        switch status {
        case .authorized:
            permissionGranted = true
            setupSession()
        case .notDetermined:
            let granted = await AVCaptureDevice.requestAccess(for: .video)
            permissionGranted = granted
            if granted { setupSession() }
        default:
            permissionGranted = false
        }
    }

    private func setupSession() {
        session.beginConfiguration()
        session.sessionPreset = .photo

        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: camera),
              session.canAddInput(input)
        else {
            session.commitConfiguration()
            return
        }
        session.addInput(input)

        if session.canAddOutput(photoOutput) {
            session.addOutput(photoOutput)
            photoOutput.isHighResolutionCaptureEnabled = true
        }

        session.commitConfiguration()

        Task.detached { [weak self] in
            self?.session.startRunning()
        }
    }

    func capturePhoto() {
        guard !isProcessing else { return }
        isProcessing = true

        Task {
            do {
                let image = try await takePhoto()
                await processImage(image)
            } catch {
                isProcessing = false
            }
        }
    }

    func importFromGallery(_ image: UIImage) {
        guard !isProcessing else { return }
        isProcessing = true
        Task {
            await processImage(image)
        }
    }

    func toggleFlash() {
        isFlashOn.toggle()
    }

    private func takePhoto() async throws -> UIImage {
        // Cancel any pending continuation before starting a new one
        photoContinuation?.resume(throwing: CaptureError.sessionNotReady)
        photoContinuation = nil

        return try await withCheckedThrowingContinuation { continuation in
            self.photoContinuation = continuation
            let settings = AVCapturePhotoSettings()
            if photoOutput.supportedFlashModes.contains(.on) {
                settings.flashMode = isFlashOn ? .on : .off
            }
            photoOutput.capturePhoto(with: settings, delegate: self)
        }
    }

    private func processImage(_ rawImage: UIImage) async {
        guard let ocrService else {
            isProcessing = false
            return
        }

        let image = downscaleImage(rawImage)

        do {
            let ocrResult = try await ocrService.recognizeText(in: image)
            let captured = CapturedImage(image: image, ocrResult: ocrResult)
            isProcessing = false
            onCapture?(captured)
        } catch {
            isProcessing = false
        }
    }

    private func downscaleImage(_ image: UIImage) -> UIImage {
        let maxDim = Configuration.maxImageDimension
        let size = image.size
        guard max(size.width, size.height) > maxDim else { return image }

        let scale = maxDim / max(size.width, size.height)
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)

        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}

// MARK: - AVCapturePhotoCaptureDelegate

extension CaptureViewModel: AVCapturePhotoCaptureDelegate {
    nonisolated func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        Task { @MainActor in
            if let error {
                photoContinuation?.resume(throwing: error)
                photoContinuation = nil
                return
            }

            guard let data = photo.fileDataRepresentation(),
                  let image = UIImage(data: data)
            else {
                photoContinuation?.resume(throwing: CaptureError.noData)
                photoContinuation = nil
                return
            }

            // Normalize orientation
            let normalized = normalizeOrientation(image)
            photoContinuation?.resume(returning: normalized)
            photoContinuation = nil
        }
    }

    private func normalizeOrientation(_ image: UIImage) -> UIImage {
        guard image.imageOrientation != .up else { return image }
        let renderer = UIGraphicsImageRenderer(size: image.size)
        return renderer.image { _ in
            image.draw(at: .zero)
        }
    }
}

enum CaptureError: Error {
    case noData
    case sessionNotReady
}
