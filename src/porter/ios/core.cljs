(ns porter.ios.core
  (:require [om.next :as om]
            [re-natal.support :as sup]
            [porter.app :as app]
            [porter.state :as state]
            [porter.ui :as ui]))

(defonce RootNode (sup/root-node! 1))
(defonce app-root (om/factory RootNode))

(defn init []
  (om/add-root! state/reconciler app/AppRoot 1)
  (.registerComponent ui/app-registry "Porter" (fn [] app-root)))
