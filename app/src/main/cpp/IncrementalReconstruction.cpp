#include "openMVG/cameras/Camera_Common.hpp"
#include "openMVG/cameras/Cameras_Common_command_line_helper.hpp"
#include "openMVG/sfm/pipelines/sequential/sequential_SfM2.hpp"
#include "openMVG/sfm/pipelines/sequential/SfmSceneInitializerMaxPair.hpp"
#include "openMVG/sfm/pipelines/sequential/SfmSceneInitializerStellar.hpp"
#include "openMVG/sfm/pipelines/sfm_features_provider.hpp"
#include "openMVG/sfm/pipelines/sfm_matches_provider.hpp"
#include "openMVG/sfm/sfm_data.hpp"
#include "openMVG/sfm/sfm_data_io.hpp"
#include "openMVG/sfm/sfm_report.hpp"
#include "openMVG/sfm/sfm_view.hpp"
#include "openMVG/system/timer.hpp"
#include "openMVG/types.hpp"

#include "third_party/stlplus3/filesystemSimplified/file_system.hpp"

#include <cstdlib>
#include <memory>
#include <string>
#include <utility>


#include <jni.h>

enum class ESfMSceneInitializer {
    INITIALIZE_EXISTING_POSES,
    INITIALIZE_MAX_PAIR,
    INITIALIZE_AUTO_PAIR,
    INITIALIZE_STELLAR
};

bool StringToEnum_ESfMSceneInitializer
        (
                const std::string &str,
                ESfMSceneInitializer &scene_initializer
        ) {
    const std::map<std::string, ESfMSceneInitializer> string_to_enum_mapping =
            {
                    {"EXISTING_POSE", ESfMSceneInitializer::INITIALIZE_EXISTING_POSES},
                    {"MAX_PAIR",      ESfMSceneInitializer::INITIALIZE_MAX_PAIR},
                    {"AUTO_PAIR",     ESfMSceneInitializer::INITIALIZE_AUTO_PAIR},
                    {"STELLAR",       ESfMSceneInitializer::INITIALIZE_STELLAR},
            };
    auto it = string_to_enum_mapping.find(str);
    if (it == string_to_enum_mapping.end())
        return false;
    scene_initializer = it->second;
    return true;
}

using namespace openMVG;
using namespace openMVG::cameras;
using namespace openMVG::sfm;

int getCameraModel(std::string sModel) {
        if(sModel == "PINHOLE_CAMERA_START") {
            return PINHOLE_CAMERA_START;
        }
        else if(sModel == "PINHOLE_CAMERA") {
            return PINHOLE_CAMERA;
        }
        else if(sModel == "PINHOLE_CAMERA_RADIAL1") {
            return PINHOLE_CAMERA_RADIAL1;
        }
        else if(sModel == "PINHOLE_CAMERA_BROWN") {
            return PINHOLE_CAMERA_BROWN;
        }
        else if(sModel == "PINHOLE_CAMERA_FISHEYE") {
            return PINHOLE_CAMERA_FISHEYE;
        }
        else if(sModel == "PINHOLE_CAMERA_END") {
            return PINHOLE_CAMERA_END;
        }
        else if(sModel == "CAMERA_SPHERICAL") {
            return CAMERA_SPHERICAL;
        }
        else {
            return PINHOLE_CAMERA_RADIAL3;
        }
}

extern "C" JNIEXPORT jstring JNICALL
Java_ir_sami_trowel_MainActivity_incrementalReconstruct(
        JNIEnv *env,
        jobject obj, jstring jsSfM_Data_Filename, jstring jsMatchesDir, jstring jsOutDir,
        jstring jsIntrinsic_refinement_options, jstring jsSfMInitializer_method, jstring jcameraModel) {

    using namespace std;
    std::cout << "Sequential/Incremental reconstruction (Engine 2)" << std::endl
              << std::endl;

    jboolean copy = true;
    std::string sSfM_Data_Filename = env->GetStringUTFChars(jsSfM_Data_Filename, &copy);
    std::string sMatchesDir = env->GetStringUTFChars(jsMatchesDir, &copy), sMatchFilename;
    std::string sOutDir = env->GetStringUTFChars(jsOutDir, &copy);
    std::string sIntrinsic_refinement_options = env->GetStringUTFChars(jsIntrinsic_refinement_options, &copy);
    std::string sSfMInitializer_method = env->GetStringUTFChars(jsSfMInitializer_method, &copy);
    int i_User_camera_model = getCameraModel(env->GetStringUTFChars(jcameraModel, &copy));
    bool b_use_motion_priors = false;
    int triangulation_method = static_cast<int>(ETriangulationMethod::DEFAULT);
    int resection_method = static_cast<int>(resection::SolverType::DEFAULT);

    if (!isValid(static_cast<ETriangulationMethod>(triangulation_method))) {
        std::cerr << "\n Invalid triangulation method" << std::endl;
        return env->NewStringUTF("FAILED");
    }

    if (!isValid(openMVG::cameras::EINTRINSIC(i_User_camera_model))) {
        std::cerr << "\n Invalid camera type" << std::endl;
        return env->NewStringUTF("FAILED");
    }

    const cameras::Intrinsic_Parameter_Type intrinsic_refinement_options =
            cameras::StringTo_Intrinsic_Parameter_Type(sIntrinsic_refinement_options);
    if (intrinsic_refinement_options == static_cast<cameras::Intrinsic_Parameter_Type>(0)) {
        std::cerr << "Invalid input for the Bundle Adjusment Intrinsic parameter refinement option"
                  << std::endl;
        return env->NewStringUTF("FAILED");
    }

    ESfMSceneInitializer scene_initializer_enum;
    if (!StringToEnum_ESfMSceneInitializer(sSfMInitializer_method, scene_initializer_enum)) {
        std::cerr << "Invalid input for the SfM initializer option" << std::endl;
        return env->NewStringUTF("FAILED");
    }

    // Load input SfM_Data scene
    SfM_Data sfm_data;
    if (!Load(sfm_data, sSfM_Data_Filename, ESfM_Data(VIEWS | INTRINSICS | EXTRINSICS))) {
        std::cerr << std::endl
                  << "The input SfM_Data file \"" << sSfM_Data_Filename << "\" cannot be read."
                  << std::endl;
        return env->NewStringUTF("FAILED");
    }

    // Init the regions_type from the image describer file (used for image regions extraction)
    using namespace openMVG::features;
    const std::string sImage_describer = stlplus::create_filespec(sMatchesDir, "image_describer",
                                                                  "json");
    std::unique_ptr<Regions> regions_type = Init_region_type_from_file(sImage_describer);
    if (!regions_type) {
        std::cerr << "Invalid: "
                  << sImage_describer << " regions type file." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    // Features reading
    std::shared_ptr<Features_Provider> feats_provider = std::make_shared<Features_Provider>();
    if (!feats_provider->load(sfm_data, sMatchesDir, regions_type)) {
        std::cerr << std::endl
                  << "Invalid features." << std::endl;
        return env->NewStringUTF("FAILED");
    }
    // Matches reading
    std::shared_ptr<Matches_Provider> matches_provider = std::make_shared<Matches_Provider>();
    if // Try to read the provided match filename or the default one (matches.f.txt/bin)
            (
            !(matches_provider->load(sfm_data, sMatchFilename) ||
              matches_provider->load(sfm_data,
                                     stlplus::create_filespec(sMatchesDir, "matches.f.txt")) ||
              matches_provider->load(sfm_data,
                                     stlplus::create_filespec(sMatchesDir, "matches.f.bin")))
            ) {
        std::cerr << std::endl
                  << "Invalid matches file." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    if (sOutDir.empty()) {
        std::cerr << "\nIt is an invalid output directory" << std::endl;
        return env->NewStringUTF("FAILED");
    }

    if (!stlplus::folder_exists(sOutDir)) {
        if (!stlplus::folder_create(sOutDir)) {
            std::cerr << "\nCannot create the output directory" << std::endl;
        }
    }

    //---------------------------------------
    // Sequential reconstruction process
    //---------------------------------------

    openMVG::system::Timer timer;

    std::unique_ptr<SfMSceneInitializer> scene_initializer;
    switch (scene_initializer_enum) {
        case ESfMSceneInitializer::INITIALIZE_AUTO_PAIR:
            std::cerr << "Not yet implemented." << std::endl;
            return env->NewStringUTF("FAILED");
            break;
        case ESfMSceneInitializer::INITIALIZE_MAX_PAIR:
            scene_initializer.reset(new SfMSceneInitializerMaxPair(sfm_data,
                                                                   feats_provider.get(),
                                                                   matches_provider.get()));
            break;
        case ESfMSceneInitializer::INITIALIZE_EXISTING_POSES:
            scene_initializer.reset(new SfMSceneInitializer(sfm_data,
                                                            feats_provider.get(),
                                                            matches_provider.get()));
            break;
        case ESfMSceneInitializer::INITIALIZE_STELLAR:
            scene_initializer.reset(new SfMSceneInitializerStellar(sfm_data,
                                                                   feats_provider.get(),
                                                                   matches_provider.get()));
            break;
        default:
            return env->NewStringUTF("FAILED");
    }
    if (!scene_initializer) {
        std::cerr << "Invalid scene initializer." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    SequentialSfMReconstructionEngine2 sfmEngine(
            scene_initializer.get(),
            sfm_data,
            sOutDir,
            stlplus::create_filespec(sOutDir, "Reconstruction_Report.html"));

    // Configure the features_provider & the matches_provider
    sfmEngine.SetFeaturesProvider(feats_provider.get());
    sfmEngine.SetMatchesProvider(matches_provider.get());

    // Configure reconstruction parameters
    sfmEngine.Set_Intrinsics_Refinement_Type(intrinsic_refinement_options);
    sfmEngine.SetUnknownCameraType(EINTRINSIC(i_User_camera_model));
    sfmEngine.Set_Use_Motion_Prior(b_use_motion_priors);
    sfmEngine.SetTriangulationMethod(static_cast<ETriangulationMethod>(triangulation_method));
    sfmEngine.SetResectionMethod(static_cast<resection::SolverType>(resection_method));

    if (sfmEngine.Process()) {
        std::cout << std::endl << " Total Ac-Sfm took (s): " << timer.elapsed() << std::endl;

        std::cout << "...Generating SfM_Report.html" << std::endl;
        Generate_SfM_Report(sfmEngine.Get_SfM_Data(),
                            stlplus::create_filespec(sOutDir, "SfMReconstruction_Report.html"));

        //-- Export to disk computed scene (data & visualizable results)
        std::cout << "...Export SfM_Data to disk." << std::endl;
        Save(sfmEngine.Get_SfM_Data(),
             stlplus::create_filespec(sOutDir, "sfm_data", ".bin"),
             ESfM_Data(ALL));

        Save(sfmEngine.Get_SfM_Data(),
             stlplus::create_filespec(sOutDir, "cloud_and_poses", ".ply"),
             ESfM_Data(ALL));

        return env->NewStringUTF("SUCCESS");
    }
    return env->NewStringUTF("FAILED");
}
