package com.ldm.ciberroyale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogrosAdapter(
    private val achievements: List<Achievement>
) : RecyclerView.Adapter<LogrosAdapter.LogroViewHolder>() {

    /**
     * ViewHolder que contiene las vistas para cada elemento de la lista (un logro).
     */
    inner class LogroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAchievementIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvAchievementTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvAchievementDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        // Infla el layout 'item_achievement.xml' para cada fila.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return LogroViewHolder(view)
    }

    /**
     * Vincula los datos de un logro específico con las vistas del ViewHolder.
     */
    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        val achievement = achievements[position]

        // Decide qué icono mostrar (bloqueado o desbloqueado)
        val iconRes = if (achievement.unlocked) {
            achievement.iconUnlockedRes
        } else {
            achievement.iconLockedRes
        }
        holder.ivIcon.setImageResource(iconRes)

        // Asigna el título y la descripción
        holder.tvTitle.text = achievement.title
        holder.tvDescription.text = achievement.description

        // Opcional: Cambia la opacidad para que los logros bloqueados se vean más tenues
        if (achievement.unlocked) {
            holder.itemView.alpha = 1.0f
        } else {
            holder.itemView.alpha = 0.6f
        }
    }

    override fun getItemCount(): Int = achievements.size
}