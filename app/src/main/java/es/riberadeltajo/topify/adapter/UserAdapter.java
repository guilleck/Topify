// UserAdapter.java
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
    private OnFriendDeleteListener onDeleteClickListener;
    private Context context;
    private int layoutResId; // Nuevo campo para el ID del layout

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public interface OnFriendDeleteListener {
        void onFriendDelete(User friend);
    }

    // Constructor para búsquedas (usará item_user.xml)
    public UserAdapter(List<User> userList, OnItemClickListener listener) {
        this.userList = userList;
        this.listener = listener;
        this.onDeleteClickListener = null; // No hay listener de eliminación
        this.layoutResId = R.layout.item_user; // Layout para búsqueda (sin botón de eliminar)
    }

    // Constructor para la lista de amigos (usará item_user_profile.xml y tendrá listener de eliminar)
    public UserAdapter(List<User> userList, OnItemClickListener listener, OnFriendDeleteListener onDeleteClickListener) {
        this.userList = userList;
        this.listener = listener;
        this.onDeleteClickListener = onDeleteClickListener;
        this.layoutResId = R.layout.item_user_profile; // Layout para amigos (con botón de eliminar)
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        // Inflar el layout según el ID pasado en el constructor
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
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
            holder.ivUserPhoto.setImageResource(R.drawable.usuario);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(user);
            }
        });

        // Solo intentar configurar el botón de eliminar si el listener no es nulo
        // (lo que implica que estamos usando el constructor de amigos y el layout correcto)
        if (onDeleteClickListener != null && holder.ivDeleteFriend != null) {
            holder.ivDeleteFriend.setVisibility(View.VISIBLE);
            holder.ivDeleteFriend.setOnClickListener(v -> {
                onDeleteClickListener.onFriendDelete(user);
            });
        } else {
            // Asegurarse de que el botón esté oculto si existe en el layout pero no se necesita
            if (holder.ivDeleteFriend != null) {
                holder.ivDeleteFriend.setVisibility(View.GONE);
                holder.ivDeleteFriend.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPhoto;
        TextView tvUserName;
        TextView tvUserEmail;
        ImageView ivDeleteFriend; // Puede ser nulo si el layout inflado no lo contiene

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            // Intentar encontrar el botón, será nulo si el layout actual no lo tiene
            ivDeleteFriend = itemView.findViewById(R.id.ivDeleteFriend);
        }
    }
}