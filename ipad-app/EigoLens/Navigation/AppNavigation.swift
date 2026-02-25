import SwiftUI

struct AppNavigation: View {
    @EnvironmentObject var container: AppContainer
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            CaptureScreen(
                onNavigate: navigate,
                onCapture: { image in
                    let id = container.setPendingCapture(image)
                    path.append(NavRoute.annotation(capturedImageId: id))
                }
            )
            .navigationDestination(for: NavRoute.self) { route in
                destinationView(for: route)
            }
        }
    }

    @ViewBuilder
    private func destinationView(for route: NavRoute) -> some View {
        switch route {
        case .capture:
            CaptureScreen(
                onNavigate: navigate,
                onCapture: { image in
                    let id = container.setPendingCapture(image)
                    path.append(NavRoute.annotation(capturedImageId: id))
                }
            )
        case .annotation(let id):
            if let image = container.getCapturedImage(id: id) {
                AnnotationScreen(
                    capturedImage: image,
                    onBack: { navigateBack() }
                )
            } else {
                Text("Image not found")
            }
        case .history:
            HistoryScreen(onBack: { navigateBack() })
        case .settings:
            SettingsScreen(onBack: { navigateBack() })
        }
    }

    private func navigate(to route: NavRoute) {
        path.append(route)
    }

    private func navigateBack() {
        if !path.isEmpty {
            path.removeLast()
        }
    }
}
