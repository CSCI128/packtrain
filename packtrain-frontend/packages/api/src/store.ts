import { observable } from "@legendapp/state";
import { ObservablePersistLocalStorage } from "@legendapp/state/persist-plugins/local-storage";
import { syncObservable } from "@legendapp/state/sync";

interface CourseStore {
  id: string;
  name: string;
  master_migration_id: string;
}

export const store$ = observable<CourseStore>();

syncObservable(store$, {
  persist: {
    name: "activeCourse",
    plugin: ObservablePersistLocalStorage,
  },
});
