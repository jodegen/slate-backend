# iOS Shortcuts – Health Import Guide

Base URL: `http://<SERVER-IP>:8080`  
Endpoint: `POST /api/health/import`  
Content-Type: `application/json`

---

## JSON Format

```json
{
  "date": "2026-05-14",
  "steps": 8432,
  "sleepMinutes": 420,
  "workouts": [
    {
      "type": "Running",
      "durationMinutes": 45,
      "startDate": "2026-05-14T07:00:00+02:00"
    }
  ]
}
```

Alle Felder sind optional — nur vorhandene Daten werden verarbeitet.

---

## Shortcut 1: Steps

**HealthKit Query**
| Feld | Wert |
|---|---|
| Type | Steps |
| Start Date | is today |
| Unit | count |
| Group By | — (kein Group By, Summe) |

**Aktion im Shortcut**
1. „Find Health Samples" → Steps → Start Date is today
2. „Calculate Statistics" → Sum → Variable `totalSteps`
3. JSON bauen:
   ```json
   { "date": "<today>", "steps": <totalSteps> }
   ```
4. POST an `/api/health/import`

---

## Shortcut 2: Sleep

**HealthKit Query**
| Feld | Wert |
|---|---|
| Type | Sleep Analysis |
| Start Date | yesterday (Schlaf beginnt vor Mitternacht) |
| End Date | today |

**Relevante Stage-Werte (Apple intern)**
| Wert | Bedeutung |
|---|---|
| 0 | In Bed |
| 1 | Asleep (generic) |
| 2 | Awake |
| 3 | Core |
| 4 | Deep |
| 5 | REM |

**Aktion im Shortcut**
1. „Find Health Samples" → Sleep Analysis → Start Date is yesterday, End Date is today
2. Für jeden Eintrag: falls Stage ≠ Awake (≠ 2) → Duration addieren
3. `sleepMinutes` = Summe der Nicht-Awake-Dauern in Minuten
4. JSON bauen:
   ```json
   { "date": "<today>", "sleepMinutes": <sleepMinutes> }
   ```
5. POST an `/api/health/import`

---

## Shortcut 3: Workouts

**HealthKit Query**
| Feld | Wert |
|---|---|
| Type | Workouts |
| Start Date | is today |

**Verfügbare Workout-Typen (werden auf TrainingType gemappt)**
| Apple Name | → TrainingType |
|---|---|
| Running | CARDIO |
| Cycling | CARDIO |
| Indoor Cycling | CARDIO |
| High Intensity Interval Training | CARDIO |
| Walking | CARDIO |
| Functional Strength Training | PUSH |
| Traditional Strength Training | PUSH |
| Core Training | PUSH |
| (alle anderen) | CARDIO |

**Aktion im Shortcut**
1. „Find Workouts" → Start Date is today
2. Für jedes Workout: Name + Duration (in Minuten) + Start Date
3. JSON bauen:
   ```json
   {
     "date": "<today>",
     "workouts": [
       { "type": "<Name>", "durationMinutes": <Dauer>, "startDate": "<ISO>" }
     ]
   }
   ```
4. POST an `/api/health/import`

---

## Shortcut 4: Alles kombiniert (empfohlen)

Einen einzigen Shortcut, der alle drei Datentypen in einem Request schickt:

```json
{
  "date": "2026-05-14",
  "steps": 10234,
  "sleepMinutes": 420,
  "workouts": [
    { "type": "Running", "durationMinutes": 45, "startDate": "2026-05-14T07:00:00+02:00" }
  ]
}
```

---

## Was passiert serverseitig

| Daten | Aktion |
|---|---|
| `steps >= 10.000` | `"steps"` wird zu `RoutineLog.completedItems` hinzugefügt |
| `sleepMinutes` | Upsert `SleepLog` für das Datum |
| `workouts` | `TrainingSession` erstellen; `TrainingDay` erstellen falls nicht vorhanden |
