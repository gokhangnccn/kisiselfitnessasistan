
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gokhan.kfa.databinding.FragmentRutinBaslatBinding

class RutinBaslatFragment : Fragment() {

    private var _binding: FragmentRutinBaslatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRutinBaslatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Burada rutin adı ve açıklaması güncellenmeli
        binding.tvRoutineName.text = "Rutin Adı"
        binding.tvRoutineDescription.text = "Rutin Açıklaması"

        // Rutin egzersizlerin listesi için bir LinearLayoutManager oluştur
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutineExercises.layoutManager = layoutManager


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
