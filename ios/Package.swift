// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "NityaMandir",
    defaultLocalization: "hi",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "NityaMandir",
            targets: ["NityaMandir"]
        ),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "NityaMandir",
            dependencies: [],
            path: "Sources/NityaMandir"
        ),
        .testTarget(
            name: "NityaMandirTests",
            dependencies: ["NityaMandir"],
            path: "Tests/NityaMandirTests"
        ),
    ]
)
