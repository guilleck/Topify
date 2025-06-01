package es.riberadeltajo.topify.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.models.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnItemClickListener listener;
    private Context context; // Añadir contexto

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public UserAdapter(List<User> userList, OnItemClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext(); // Obtener contexto aquí
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getNombre());
        holder.tvUserEmail.setText(user.getEmail());

        if (user.getFoto() != null && !user.getFoto().isEmpty()) {
            Glide.with(context).load(user.getFoto()).into(holder.ivUserPhoto);
        } else {
            holder.ivUserPhoto.setImageResource(R.drawable.usuario); // Imagen por defecto
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPhoto;
        TextView tvUserName;
        TextView tvUserEmail;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto); // Asegúrate de que estos IDs existen en item_user.xml
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
        }
    }
}