import { observable } from "@legendapp/state";
import { ObservablePersistLocalStorage } from "@legendapp/state/persist-plugins/local-storage";
import { syncObservable } from "@legendapp/state/sync";

interface CourseStore {
  id: string;
  name: string;
}

export const store$ = observable<CourseStore>();

syncObservable(store$, {
  persist: {
    name: "activeCourse",
    plugin: ObservablePersistLocalStorage,
  },
});
// todo - need a better way to grab the user access_token
