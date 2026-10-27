import { TextDecoder, TextEncoder } from 'node:util';

// jsdom does not always expose these globals, but the audit client uses
// TextEncoder to bound the metadata payload size.
if (typeof globalThis.TextEncoder === 'undefined') {
  Object.assign(globalThis, { TextEncoder, TextDecoder });
}
