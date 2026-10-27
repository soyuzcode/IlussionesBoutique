import { API_BASE_URL } from './config';

export type AuditEventType =
  | 'UI_SESSION_STARTED'
  | 'UI_LOGIN_SUBMITTED'
  | 'UI_LOGOUT_CLICKED'
  | 'UI_SECTION_VIEWED'
  | 'UI_PRODUCT_CATALOG_VIEWED'
  | 'UI_APPOINTMENT_DETAIL_VIEWED'
  | 'UI_APPOINTMENT_FORM_OPENED'
  | 'UI_APPOINTMENT_FORM_VALIDATION_FAILED'
  | 'UI_APPOINTMENT_FORM_SUBMITTED'
  | 'UI_APPOINTMENT_CREATE_FAILED'
  | 'UI_API_REQUEST_FAILED'
  | 'UI_UNEXPECTED_ERROR';

export type AuditEntityType =
  | 'APPLICATION'
  | 'AUTH'
  | 'CUSTOMER'
  | 'APPOINTMENT'
  | 'ORDER'
  | 'PRODUCT';

export type AuditMetadataKey =
  | 'SECTION'
  | 'OPERATION'
  | 'HTTP_STATUS'
  | 'ERROR_CODE'
  | 'ERROR_NAME'
  | 'FIELD'
  | 'REASON'
  | 'AUTH_METHOD'
  | 'CUSTOMER_ID'
  | 'APPOINTMENT_STATUS'
  | 'PRODUCT_COUNT'
  | 'HAS_DELIVERY_DATE';

export type AuditMetadataValue = string | number | boolean;
export type AuditMetadata = Partial<
  Record<AuditMetadataKey, AuditMetadataValue>
>;

export type AuditOperation =
  | 'LOAD_APPOINTMENTS'
  | 'LOAD_CUSTOMERS'
  | 'CREATE_CUSTOMER'
  | 'CREATE_APPOINTMENT'
  | 'RELOAD_APPOINTMENTS'
  | 'LOAD_ORDERS'
  | 'CREATE_ORDER'
  | 'DELETE_ORDER'
  | 'DELETE_CUSTOMER';

export type AuditOperationType = AuditOperation;

export type AuditErrorCode =
  | 'NETWORK_ERROR'
  | 'HTTP_ERROR'
  | 'TIMEOUT'
  | 'INVALID_RESPONSE'
  | 'VALIDATION_ERROR'
  | 'UNKNOWN_ERROR';

export interface AuditEventInput {
  eventType: AuditEventType;
  entityType: AuditEntityType;
  entityId?: string;
  metadata?: AuditMetadata;
}

interface AuditEventPayload {
  eventType: AuditEventType;
  entityType: AuditEntityType;
  entityId?: string;
  metadata: Record<string, AuditMetadataValue>;
}

const SESSION_STORAGE_KEY = 'ilussiones.audit.sessionId';
const SESSION_ID_PATTERN = /^[A-Za-z0-9_-]{8,64}$/;
const ENTITY_ID_PATTERN = /^[A-Za-z0-9._:-]{1,128}$/;
const SAFE_TEXT_PATTERN = /^[\p{L}\p{N} .,:_/@+-]{1,120}$/u;
const ERROR_NAME_PATTERN = /^[A-Za-z][A-Za-z0-9]{0,39}$/;
const MAX_METADATA_ENTRIES = 10;
const MAX_METADATA_JSON_BYTES = 2048;
const MAX_EVENTS_PER_MINUTE = 40;
const DEDUPE_WINDOW_MS = 1_000;
const REQUEST_TIMEOUT_MS = 3_000;

const EVENT_METADATA_KEYS: Record<AuditEventType, readonly AuditMetadataKey[]> = {
  UI_SESSION_STARTED: [],
  UI_LOGIN_SUBMITTED: ['AUTH_METHOD'],
  UI_LOGOUT_CLICKED: [],
  UI_SECTION_VIEWED: ['SECTION'],
  UI_PRODUCT_CATALOG_VIEWED: ['SECTION'],
  UI_APPOINTMENT_DETAIL_VIEWED: [],
  UI_APPOINTMENT_FORM_OPENED: [],
  UI_APPOINTMENT_FORM_VALIDATION_FAILED: ['FIELD', 'REASON'],
  UI_APPOINTMENT_FORM_SUBMITTED: [],
  UI_APPOINTMENT_CREATE_FAILED: ['REASON'],
  UI_API_REQUEST_FAILED: ['OPERATION', 'HTTP_STATUS', 'ERROR_CODE'],
  UI_UNEXPECTED_ERROR: ['ERROR_NAME'],
};

const EVENT_ENTITY_TYPES: Record<AuditEventType, readonly AuditEntityType[]> = {
  UI_SESSION_STARTED: ['AUTH'],
  UI_LOGIN_SUBMITTED: ['AUTH'],
  UI_LOGOUT_CLICKED: ['AUTH'],
  UI_SECTION_VIEWED: ['APPLICATION'],
  UI_PRODUCT_CATALOG_VIEWED: ['PRODUCT'],
  UI_APPOINTMENT_DETAIL_VIEWED: ['APPOINTMENT'],
  UI_APPOINTMENT_FORM_OPENED: ['APPOINTMENT'],
  UI_APPOINTMENT_FORM_VALIDATION_FAILED: ['APPOINTMENT'],
  UI_APPOINTMENT_FORM_SUBMITTED: ['APPOINTMENT'],
  UI_APPOINTMENT_CREATE_FAILED: ['APPOINTMENT'],
  UI_API_REQUEST_FAILED: ['CUSTOMER', 'APPOINTMENT', 'ORDER'],
  UI_UNEXPECTED_ERROR: ['APPLICATION'],
};

const VALUE_SETS: Partial<Record<AuditMetadataKey, readonly string[]>> = {
  SECTION: ['INICIO', 'CLIENTES', 'PEDIDOS', 'INVENTARIO'],
  OPERATION: [
    'LOAD_APPOINTMENTS',
    'LOAD_CUSTOMERS',
    'CREATE_CUSTOMER',
    'CREATE_APPOINTMENT',
    'RELOAD_APPOINTMENTS',
    'LOAD_ORDERS',
    'CREATE_ORDER',
    'DELETE_ORDER',
    'DELETE_CUSTOMER',
  ],
  ERROR_CODE: [
    'NETWORK_ERROR',
    'HTTP_ERROR',
    'TIMEOUT',
    'INVALID_RESPONSE',
    'VALIDATION_ERROR',
    'UNKNOWN_ERROR',
  ],
  FIELD: ['CLIENT', 'TIME', 'PERIOD', 'TYPE'],
  REASON: [
    'REQUIRED',
    'FORMAT',
    'NOT_FOUND',
    'INVALID_DATA',
    'PERSISTENCE_ERROR',
    'DUPLICATE',
    'DATABASE_ERROR',
  ],
  AUTH_METHOD: ['PASSWORD', 'GOOGLE'],
  APPOINTMENT_STATUS: ['SCHEDULED', 'COMPLETED', 'CANCELLED'],
};

let memorySessionId: string | null = null;
let auditFailureReported = false;
const recentEvents = new Map<string, number>();
const recentEventTimes: number[] = [];

function generateSessionId(): string {
  if (
    typeof crypto !== 'undefined' &&
    typeof crypto.randomUUID === 'function'
  ) {
    return crypto.randomUUID();
  }
  return `s-${Date.now().toString(36)}-${Math.random()
    .toString(36)
    .slice(2, 12)}`;
}

export function getAuditSessionId(): string {
  if (memorySessionId) {
    return memorySessionId;
  }

  try {
    const existing = window.sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (existing && SESSION_ID_PATTERN.test(existing)) {
      memorySessionId = existing;
      return existing;
    }

    const created = generateSessionId();
    window.sessionStorage.setItem(SESSION_STORAGE_KEY, created);
    memorySessionId = created;
    return created;
  } catch {
    // Storage can be unavailable in privacy modes. An in-memory session id
    // still provides correlation for this page lifetime.
    memorySessionId = generateSessionId();
    return memorySessionId;
  }
}

function sanitiseMetadata(
  eventType: AuditEventType,
  metadata?: AuditMetadata
): Record<string, AuditMetadataValue> {
  if (!metadata) {
    return {};
  }

  const allowedKeys = new Set(EVENT_METADATA_KEYS[eventType]);
  const result: Record<string, AuditMetadataValue> = {};

  for (const [rawKey, value] of Object.entries(metadata)) {
    if (Object.keys(result).length >= MAX_METADATA_ENTRIES) {
      break;
    }

    const key = rawKey as AuditMetadataKey;
    if (!allowedKeys.has(key) || value === undefined) {
      continue;
    }

    if (typeof value === 'boolean') {
      if (key === 'HAS_DELIVERY_DATE') {
        result[key] = value;
      }
      continue;
    }

    if (typeof value === 'number') {
      if (!Number.isInteger(value)) {
        continue;
      }
      if (key === 'HTTP_STATUS' && (value < 100 || value > 599)) {
        continue;
      }
      if (key === 'PRODUCT_COUNT' && (value < 0 || value > 10_000)) {
        continue;
      }
      if (key === 'HTTP_STATUS' || key === 'PRODUCT_COUNT') {
        result[key] = value;
      }
      continue;
    }

    const text = value.trim();
    if (text.length === 0 || text.length > 120) {
      continue;
    }

    if (key === 'ERROR_NAME') {
      if (ERROR_NAME_PATTERN.test(text)) {
        result[key] = text;
      }
      continue;
    }

    if (!SAFE_TEXT_PATTERN.test(text)) {
      continue;
    }

    const allowedValues = VALUE_SETS[key];
    if (allowedValues && !allowedValues.includes(text)) {
      continue;
    }

    result[key] = text;
  }

  if (new TextEncoder().encode(JSON.stringify(result)).length > MAX_METADATA_JSON_BYTES) {
    return {};
  }

  return result;
}

function isRateLimited(now: number): boolean {
  while (
    recentEventTimes.length > 0 &&
    now - recentEventTimes[0] >= 60_000
  ) {
    recentEventTimes.shift();
  }

  if (recentEventTimes.length >= MAX_EVENTS_PER_MINUTE) {
    return true;
  }

  recentEventTimes.push(now);
  return false;
}

function isDuplicate(signature: string, now: number): boolean {
  const previous = recentEvents.get(signature);
  if (previous !== undefined && now - previous < DEDUPE_WINDOW_MS) {
    return true;
  }

  recentEvents.set(signature, now);
  if (recentEvents.size > 100) {
    for (const [key, timestamp] of recentEvents) {
      if (now - timestamp >= DEDUPE_WINDOW_MS) {
        recentEvents.delete(key);
      }
    }
  }
  return false;
}

async function sendEvent(payload: AuditEventPayload): Promise<void> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    await fetch(`${API_BASE_URL}/audit/events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Audit-Session-Id': getAuditSessionId(),
      },
      body: JSON.stringify(payload),
      credentials: 'omit',
      keepalive: true,
      signal: controller.signal,
    });
  } catch {
    // Logging is best-effort. Never retry, never show a user-facing error and
    // never call the audit client again from this failure path.
    if (!auditFailureReported) {
      auditFailureReported = true;
      console.debug('Audit logging is currently unavailable.');
    }
  } finally {
    window.clearTimeout(timeout);
  }
}

/**
 * Fire-and-forget frontend audit event. Never throws and never returns a
 * promise that callers must handle.
 */
export function auditEvent(input: AuditEventInput): void {
  const allowedEntityTypes = EVENT_ENTITY_TYPES[input.eventType];
  if (!allowedEntityTypes || !allowedEntityTypes.includes(input.entityType)) {
    return;
  }

  const entityId =
    input.entityId && ENTITY_ID_PATTERN.test(input.entityId)
      ? input.entityId
      : undefined;

  const metadata = sanitiseMetadata(input.eventType, input.metadata);
  const now = Date.now();
  // Include the bounded metadata so two different failures (for example
  // LOAD_CUSTOMERS and CREATE_APPOINTMENT) are not collapsed into one event.
  const signature = JSON.stringify({
    eventType: input.eventType,
    entityType: input.entityType,
    entityId: entityId ?? null,
    metadata,
  });

  if (isRateLimited(now) || isDuplicate(signature, now)) {
    return;
  }

  const payload: AuditEventPayload = {
    eventType: input.eventType,
    entityType: input.entityType,
    metadata,
  };

  if (entityId) {
    payload.entityId = entityId;
  }

  void sendEvent(payload);
}

export function recordApiFailure(
  entityType: AuditEntityType,
  operation: AuditOperation,
  errorCode: AuditErrorCode,
  httpStatus?: number
): void {
  const metadata: AuditMetadata = {
    OPERATION: operation,
    ERROR_CODE: errorCode,
  };

  if (httpStatus !== undefined) {
    metadata.HTTP_STATUS = httpStatus;
  }

  auditEvent({
    eventType: 'UI_API_REQUEST_FAILED',
    entityType,
    metadata,
  });
}

export function sanitiseErrorName(error: unknown): string {
  const name =
    error instanceof Error && error.name && ERROR_NAME_PATTERN.test(error.name)
      ? error.name
      : 'UnknownError';
  return name;
}
