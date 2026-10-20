package com.example.cortinas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AdicionalInput(val descripcion: String = "", val precio: String = "")

@Composable
fun MechanismControls(
    esDosCuerpos: Boolean,
    mandoLado: String,
    onMandoLadoChange: (String) -> Unit,
    caida: String,
    onCaidaChange: (String) -> Unit,
    costoMotor: String,
    onCostoMotorChange: (String) -> Unit,
    adicionales: List<AdicionalInput>,
    onAdicionalesChange: (List<AdicionalInput>) -> Unit,
    modifier: Modifier = Modifier
) {
    var hasMotor by remember { mutableStateOf(costoMotor.isNotBlank() && (costoMotor.toDoubleOrNull() ?: 0.0) > 0.0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Detalles de Confección & Mecanismo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // En dos cuerpos se asume una cadena a cada lado.
        if (!esDosCuerpos) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isIzquierda = mandoLado.contains("Izquierda", true)
            val isDerecha = mandoLado.contains("Derecha", true)

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isIzquierda) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onMandoLadoChange("Izquierda") }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatAlignLeft,
                        contentDescription = null,
                        tint = if (isIzquierda) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Cadena Izquierda",
                        fontSize = 12.sp,
                        fontWeight = if (isIzquierda) FontWeight.Bold else FontWeight.Medium,
                        color = if (isIzquierda) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isDerecha) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onMandoLadoChange("Derecha") }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatAlignRight,
                        contentDescription = null,
                        tint = if (isDerecha) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Cadena Derecha",
                        fontSize = 12.sp,
                        fontWeight = if (isDerecha) FontWeight.Bold else FontWeight.Medium,
                        color = if (isDerecha) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Selector de Caída: "Normal" o "Rollo invertido"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isNormal = caida.equals("Normal", ignoreCase = true) || caida.equals("Rollo normal", ignoreCase = true) || caida.contains("detrás", true)
            val isInvertida = caida.equals("Invertida", ignoreCase = true) || caida.equals("Rollo invertido", ignoreCase = true) || caida.contains("delante", true)

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isNormal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onCaidaChange("Normal") }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (isNormal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Rollo normal",
                        fontSize = 12.sp,
                        fontWeight = if (isNormal) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNormal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isInvertida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onCaidaChange("Invertida") }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (isInvertida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Rollo invertido",
                        fontSize = 12.sp,
                        fontWeight = if (isInvertida) FontWeight.Bold else FontWeight.Medium,
                        color = if (isInvertida) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Switch de Motorización con animación de despliegue
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = if (hasMotor) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("¿Cortina Motorizada?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Motor tubular con control remoto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = hasMotor,
                        onCheckedChange = { checked ->
                            hasMotor = checked
                            if (!checked) onCostoMotorChange("")
                            else if (costoMotor.isBlank()) onCostoMotorChange("220")
                        }
                    )
                }

                AnimatedVisibility(
                    visible = hasMotor,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedTextField(
                        value = costoMotor,
                        onValueChange = onCostoMotorChange,
                        label = { Text("Valor del Motor (USD)") },
                        placeholder = { Text("Ej: 220") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true
                    )
                }
            }
        }

        // 4. Adicionales con descripción y precio individual
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Otros adicionales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onAdicionalesChange(adicionales + AdicionalInput()) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
            adicionales.forEachIndexed { index, adicional ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = adicional.descripcion,
                        onValueChange = { value ->
                            onAdicionalesChange(adicionales.toMutableList().also { it[index] = adicional.copy(descripcion = value) })
                        },
                        label = { Text("Descripción") },
                        placeholder = { Text("Ej: Cenefa") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = adicional.precio,
                            onValueChange = { value ->
                                onAdicionalesChange(adicionales.toMutableList().also { it[index] = adicional.copy(precio = value) })
                            },
                            label = { Text("Precio ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            onAdicionalesChange(adicionales.filterIndexed { itemIndex, _ -> itemIndex != index })
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Quitar adicional", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
