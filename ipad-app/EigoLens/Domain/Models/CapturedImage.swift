import UIKit

struct CapturedImage: Identifiable {
    let id: UUID
    let image: UIImage
    var ocrResult: OCRResult
    let timestamp: Date

    init(id: UUID = UUID(), image: UIImage, ocrResult: OCRResult, timestamp: Date = .now) {
        self.id = id
        self.image = image
        self.ocrResult = ocrResult
        self.timestamp = timestamp
    }
}
