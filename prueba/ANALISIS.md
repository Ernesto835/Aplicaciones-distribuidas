# Componente de Análisis y Defensa Técnica - Unidad 1
[cite_start]**Asignatura:** Aplicaciones Distribuidas (ISR-701) [cite: 7]
**Estudiante:** [Tu Nombre y Apellido]
[cite_start]**Institución:** Universidad Técnica Estatal de Quevedo [cite: 1]

---

### 1. Ante una interrupción de comunicación entre dos nodos, ¿qué propiedad del teorema CAP privilegia su implementación y por qué? [cite_start]Justifíquelo con el comportamiento observado en su prototipo. [cite: 43, 44]

**Respuesta:** Privilegia la **Disponibilidad (A)** ante la **Tolerancia a Particiones (P)**, siendo un sistema **AP**.

[cite_start]**Justificación:** Al apagar forzosamente el `Nodo 3` [cite: 12][cite_start], el sistema no se detiene[cite: 31]. [cite_start]El cliente puede seguir enviando solicitudes al `Nodo 1` o `Nodo 2` [cite: 12][cite_start], y estos procesan y confirman las operaciones de manera local[cite: 25, 31]. [cite_start]Se sacrifica la consistencia global inmediata para mantener el servicio activo[cite: 31].

---

### [cite_start]2. ¿Qué falacias de la computación distribuida tuvo que considerar al delimitar los mensajes y al definir los tiempos de espera de los latidos? [cite: 45]

**Respuesta:** * **La red es confiable:** Se implementaron bloques `try-catch` para capturar excepciones de conectividad y evitar que el sistema colapse ante fallos del canal.
* [cite_start]**La latencia es cero:** Se fijó un umbral de espera de **5 segundos** en los latidos antes de declarar un nodo como caído[cite: 31]. Esto evita falsas detecciones por retrasos temporales en la red.
* [cite_start]**El ancho de banda es infinito:** Se diseñó un DTO estructurado (`Mensaje.java`) en lugar de texto plano masivo, permitiendo una delimitación exacta por serialización de objetos de Java para evitar el truncamiento o solapamiento de bytes[cite: 24].

---

### 3. ¿Qué tipos de transparencia (ubicación, acceso, fallos, replicación) ofrece o no ofrece su solución? [cite_start]Argumente cada caso. [cite: 46]

**Respuesta:**
* [cite_start]**Transparencia de Acceso (Ofrece):** Clientes y nodos usan la misma interfaz de comunicación y el mismo formato de datos mediante Sockets TCP[cite: 20].
* [cite_start]**Transparencia de Fallos (Ofrece):** Los nodos detectan la caída del coordinador mediante la ausencia de latidos y eligen un reemplazo de forma automática y autónoma[cite: 31, 35].
* [cite_start]**Transparencia de Ubicación (No ofrece completamente):** Aunque oculta la IP usando `localhost`[cite: 15], el cliente requiere conocer el número de puerto específico del nodo para redirigir una petición si el canal anterior falla.
* [cite_start]**Transparencia de Replicación (No ofrece):** El prototipo ordena cronológicamente los eventos en logs lógicos [cite: 28][cite_start], pero no realiza una duplicación transaccional o espejo de los datos en caliente entre los discos duros de las réplicas[cite: 14].

---

### [cite_start]4. Proponga un acuerdo de nivel de servicio (SLA) de disponibilidad para este sistema y calcule el tiempo de inactividad anual admisible que implicaría. [cite: 47]

**Respuesta:** Se propone un SLA de disponibilidad del **99.9%** ("tres nueves").

**Cálculo:**
* Horas totales del año: $$365 \text{ días} \times 24 \text{ horas/día} = 8760 \text{ horas}$$
* Tiempo de inactividad máximo permitido (0.1%): $$8760 \text{ horas} \times 0.001 = 8.76 \text{ horas}$$
* Conversión de fracción: $$0.76 \text{ horas} \times 60 = 45.6 \text{ minutos}$$y$$0.6 \text{ minutos} \times 60 = 36 \text{ segundos}$$

**Resultado:** El tiempo de inactividad anual máximo permitido es de **8 horas, 45 minutos y 36 seconds**.

---

### [cite_start]5. Si reemplazara el algoritmo Bully por un consenso tipo Raft, ¿qué ganaría y qué costo introduciría? [cite: 48]

**Respuesta:**
* [cite_start]**Ganancia:** Consistencia robusta de datos[cite: 13]. Evita el problema de *Split-Brain* (cerebro dividido) mediante quórum estricto y asegura que el log se replique de manera idéntica en la mayoría antes de confirmarse.
* **Costo:** Alta complejidad algorítmica en la lógica del código (estados de líder, seguidor y candidato) y un elevado *overhead* (sobrecarga) de red por el intercambio continuo de mensajes de control RPC.