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

    /** ViewHolder que agrupa las vistas de cada ítem (icono, título, descripción) */
    inner class LogroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAchievementIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvAchievementTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvAchievementDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return LogroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        val achievement = achievements[position]

        // Si está desbloqueado, mostramos iconUnlockedRes; si no, iconLockedRes
        val iconRes = if (achievement.unlocked) {
            achievement.iconUnlockedRes
        } else {
            achievement.iconLockedRes
        }
        holder.ivIcon.setImageResource(iconRes)

        // Poner el título y la descripción
        holder.tvTitle.text = achievement.title
        holder.tvDescription.text = achievement.description

        // (Opcional) Podrías cambiar el alpha/textColor si está bloqueado
        if (achievement.unlocked) {
            holder.ivIcon.alpha = 1.0f
            holder.tvTitle.alpha = 1.0f
            holder.tvDescription.alpha = 1.0f
        } else {
            holder.ivIcon.alpha = 0.5f
            holder.tvTitle.alpha = 0.6f
            holder.tvDescription.alpha = 0.6f
        }

        // (Opcional) Si quieres responder al click en cada logro:
        holder.itemView.setOnClickListener {
            if (!achievement.unlocked) {
                // Por ejemplo, mostrar un Toast que indique “Aún no desbloqueado”
                // Toast.makeText(holder.itemView.context, "Completa el tema/nivel para desbloquear", Toast.LENGTH_SHORT).show()
            } else {
                // Si quisieras mostrar detalles o fecha de desbloqueo, etc.
            }
        }
    }

    override fun getItemCount(): Int = achievements.size
}
