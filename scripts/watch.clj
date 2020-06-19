(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'tran-cljs.core
   :output-to "out/tran_cljs.js"
   :output-dir "out"})
