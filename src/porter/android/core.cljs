(ns porter.android.core
  (:require [om.next :as om :refer-macros [defui]]
            [re-natal.support :as sup]
            [porter.app :as app]
            [porter.state :as state]
            [porter.ui :as ui]))

(defonce RootNode (sup/root-node! 1))
(defonce app-root (om/factory RootNode))

(defn init []
  (om/add-root! state/reconciler app/AppRoot 1)
  (.registerComponent ui/app-registry "Porter" (fn [] app-root)))
