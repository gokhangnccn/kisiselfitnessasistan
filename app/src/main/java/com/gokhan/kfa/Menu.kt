package com.gokhan.kfa

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class Menu : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_menu, container, false)


        val buttonCikis: Button = view.findViewById(R.id.button_cikis)
        buttonCikis.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(activity, GirisEkrani::class.java)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = Menu()
    }
}
