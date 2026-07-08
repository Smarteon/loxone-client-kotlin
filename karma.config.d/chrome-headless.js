// Custom ChromeHeadless launcher; the flags let headless Chrome run in CI/sandboxes.
// Set CHROME_BIN if the binary isn't auto-detected on PATH.
config.set({
    browsers: ["ChromeHeadlessCI"],
    customLaunchers: {
        ChromeHeadlessCI: {
            base: "ChromeHeadless",
            flags: [
                "--no-sandbox",
                "--disable-gpu",
                "--disable-dev-shm-usage",
            ],
        },
    },
});
