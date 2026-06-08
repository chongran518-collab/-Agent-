declare module 'vuex' {
  import { StoreOptions } from 'vuex/types'
  export function createStore<S>(options: StoreOptions<S>): any
  export type Commit = any
}