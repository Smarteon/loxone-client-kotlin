// Custom ChromeHeadless launcher; the flags let headless Chrome run in CI/sandboxes.
// Set CHROME_BIN if the binary isn't auto-detected on PATH.
(function (config) {
    var flags = ["--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage"];

    // Acceptance runs (LOX_ADDRESS set) hit a cross-origin Miniserver; disable web security so the
    // browser doesn't CORS-block the getPublicKey GET (needs its own --user-data-dir to take effect).
    if (process.env.LOX_ADDRESS) {
        var os = require("os");
        var path = require("path");
        flags.push(
            "--disable-web-security",
            "--disable-site-isolation-trials",
            "--user-data-dir=" + path.join(os.tmpdir(), "lox-at-chrome")
        );
    }

    config.set({
        browsers: ["ChromeHeadlessCI"],
        customLaunchers: {
            ChromeHeadlessCI: {
                base: "ChromeHeadless",
                flags: flags,
            },
        },
    });
})(config);
