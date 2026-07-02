// The browser has no process.env, so bake the acceptance credentials into the bundle at build
// time; unset vars become null and the spec self-skips. (Node reads process.env directly.)
(function (config) {
    var webpack = require("webpack");
    config.plugins = config.plugins || [];
    config.plugins.push(
        new webpack.DefinePlugin({
            "process.env.LOX_ADDRESS": JSON.stringify(process.env.LOX_ADDRESS || null),
            "process.env.LOX_USER": JSON.stringify(process.env.LOX_USER || null),
            "process.env.LOX_PASS": JSON.stringify(process.env.LOX_PASS || null),
        })
    );
})(config);
