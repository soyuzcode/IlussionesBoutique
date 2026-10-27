import { TIME_SLOTS, isPastSlot, slotKey, type TimeSlot } from '../../utils/dates';

interface TimeSlotPickerProps {
  date: string;
  value: TimeSlot | null;
  onChange: (slot: TimeSlot) => void;
}

export function TimeSlotPicker({ date, value, onChange }: TimeSlotPickerProps) {
  return (
    <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 max-h-44 overflow-y-auto p-1">
      {TIME_SLOTS.map((slot) => {
        const selected = value !== null && slotKey(value) === slotKey(slot);
        const past = isPastSlot(slot, date);
        return (
          <button
            key={slotKey(slot)}
            type="button"
            disabled={past}
            onClick={() => onChange(slot)}
            className={`py-2 rounded-lg text-xs font-bold border transition cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed ${
              selected
                ? 'bg-pink-600 text-white border-pink-600'
                : 'bg-gray-50 text-gray-700 border-gray-200 hover:border-pink-400'
            }`}
          >
            {slot.time} {slot.period}
          </button>
        );
      })}
    </div>
  );
}

export default TimeSlotPicker;