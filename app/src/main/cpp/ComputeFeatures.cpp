// This file is part of OpenMVG, an Open Multiple View Geometry C++ library.

// Copyright (c) 2012, 2013 Pierre MOULON.

// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

// The <cereal/archives> headers are special and must be included first.
#include <cereal/archives/json.hpp>

#include "openMVG/features/akaze/image_describer_akaze_io.hpp"

#include "openMVG/features/sift/SIFT_Anatomy_Image_Describer_io.hpp"
#include "openMVG/image/image_io.hpp"
#include "openMVG/features/regions_factory_io.hpp"
#include "openMVG/sfm/sfm_data.hpp"
#include "openMVG/sfm/sfm_data_io.hpp"
#include "openMVG/system/timer.hpp"

#include "third_party/progress/progress_display.hpp"
#include "third_party/stlplus3/filesystemSimplified/file_system.hpp"

#include "nonFree/sift/SIFT_describer_io.hpp"

#include <cereal/details/helpers.hpp>

#include <atomic>
#include <cstdlib>
#include <fstream>
#include <string>

#include <jni.h>
#include <string>

using namespace openMVG;
using namespace openMVG::image;
using namespace openMVG::features;
using namespace openMVG::sfm;
using namespace std;

features::EDESCRIBER_PRESET stringToEnum(const std::string & sPreset)
{
    features::EDESCRIBER_PRESET preset;
    if (sPreset == "NORMAL")
        preset = features::NORMAL_PRESET;
    else
    if (sPreset == "HIGH")
        preset = features::HIGH_PRESET;
    else
    if (sPreset == "ULTRA")
        preset = features::ULTRA_PRESET;
    else
        preset = features::EDESCRIBER_PRESET(-1);
    return preset;
}

/// - Compute view image description (feature & descriptor extraction)
/// - Export computed data

extern "C" JNIEXPORT jstring JNICALL
Java_ir_sami_trowel_MainActivity_computeFeatures(
        JNIEnv *env,
        jobject /* this */, jstring jsSfM_Data_Filename, jstring jsOutDir, jstring jbUpRight, jstring jsImage_Describer_Method, jstring jsFeaturePreset) {

    jboolean copy = true;
    std::string sSfM_Data_Filename = env->GetStringUTFChars(jsSfM_Data_Filename, &copy);
    std::string sOutDir = env->GetStringUTFChars(jsOutDir, &copy);
    bool bUpRight = env->GetStringUTFChars(jbUpRight, &copy)[0] == '1';
    std::string sImage_Describer_Method = env->GetStringUTFChars(jsImage_Describer_Method, &copy);
    bool bForce = false;
    std::string sFeaturePreset = env->GetStringUTFChars(jsFeaturePreset, &copy);
    // required

    if (sOutDir.empty())  {
        std::cerr << "\nIt is an invalid output directory" << std::endl;
        return env->NewStringUTF("FAILED");
    }

    // Create output dir
    if (!stlplus::folder_exists(sOutDir))
    {
        if (!stlplus::folder_create(sOutDir))
        {
            std::cerr << "Cannot create output directory" << std::endl;
            return env->NewStringUTF("FAILED");
        }
    }

    //---------------------------------------
    // a. Load input scene
    //---------------------------------------
    SfM_Data sfm_data;
    if (!Load(sfm_data, sSfM_Data_Filename, ESfM_Data(VIEWS|INTRINSICS))) {
        std::cerr << std::endl
                  << "The input file \""<< sSfM_Data_Filename << "\" cannot be read" << std::endl;
        return env->NewStringUTF("FAILED");
    }

    // b. Init the image_describer
    // - retrieve the used one in case of pre-computed features
    // - else create the desired one

    using namespace openMVG::features;
    std::unique_ptr<Image_describer> image_describer;

    const std::string sImage_describer = stlplus::create_filespec(sOutDir, "image_describer", "json");
    if (!bForce && stlplus::is_file(sImage_describer))
    {
        // Dynamically load the image_describer from the file (will restore old used settings)
        std::ifstream stream(sImage_describer.c_str());
        if (!stream.is_open())
            return env->NewStringUTF("FAILED");

        try
        {
            cereal::JSONInputArchive archive(stream);
            archive(cereal::make_nvp("image_describer", image_describer));
        }
        catch (const cereal::Exception & e)
        {
            std::cerr << e.what() << std::endl
                      << "Cannot dynamically allocate the Image_describer interface." << std::endl;
            return env->NewStringUTF("FAILED");
        }
    }
    else
    {
        // Create the desired Image_describer method.
        // Don't use a factory, perform direct allocation
        if (sImage_Describer_Method == "SIFT")
        {
            image_describer.reset(new SIFT_Image_describer
                                          (SIFT_Image_describer::Params(), !bUpRight));
        }
        else
        if (sImage_Describer_Method == "SIFT_ANATOMY")
        {
            image_describer.reset(
                    new SIFT_Anatomy_Image_describer(SIFT_Anatomy_Image_describer::Params()));
        }
        else
        if (sImage_Describer_Method == "AKAZE_FLOAT")
        {
            image_describer = AKAZE_Image_describer::create
                    (AKAZE_Image_describer::Params(AKAZE::Params(), AKAZE_MSURF), !bUpRight);
        }
        else
        if (sImage_Describer_Method == "AKAZE_MLDB")
        {
            image_describer = AKAZE_Image_describer::create
                    (AKAZE_Image_describer::Params(AKAZE::Params(), AKAZE_MLDB), !bUpRight);
        }
        if (!image_describer)
        {
            std::cerr << "Cannot create the designed Image_describer:"
                      << sImage_Describer_Method << "." << std::endl;
            return env->NewStringUTF("FAILED");
        }
        else
        {
            if (!sFeaturePreset.empty())
                if (!image_describer->Set_configuration_preset(stringToEnum(sFeaturePreset)))
                {
                    std::cerr << "Preset configuration failed." << std::endl;
                    return env->NewStringUTF("FAILED");
                }
        }

        // Export the used Image_describer and region type for:
        // - dynamic future regions computation and/or loading
        {
            std::ofstream stream(sImage_describer.c_str());
            if (!stream.is_open())
                return env->NewStringUTF("FAILED");

            cereal::JSONOutputArchive archive(stream);
            archive(cereal::make_nvp("image_describer", image_describer));
            auto regionsType = image_describer->Allocate();
            archive(cereal::make_nvp("regions_type", regionsType));
        }
    }

    // Feature extraction routines
    // For each View of the SfM_Data container:
    // - if regions file exists continue,
    // - if no file, compute features
    {
        system::Timer timer;
        Image<unsigned char> imageGray;

        C_Progress_display my_progress_bar(sfm_data.GetViews().size(),
                                           std::cout, "\n- EXTRACT FEATURES -\n" );

        // Use a boolean to track if we must stop feature extraction
        std::atomic<bool> preemptive_exit(false);

        for (int i = 0; i < static_cast<int>(sfm_data.views.size()); ++i)
        {
            Views::const_iterator iterViews = sfm_data.views.begin();
            std::advance(iterViews, i);
            const View * view = iterViews->second.get();
            const std::string
                    sView_filename = stlplus::create_filespec(sfm_data.s_root_path, view->s_Img_path),
                    sFeat = stlplus::create_filespec(sOutDir, stlplus::basename_part(sView_filename), "feat"),
                    sDesc = stlplus::create_filespec(sOutDir, stlplus::basename_part(sView_filename), "desc");

            // If features or descriptors file are missing, compute them
            if (!preemptive_exit && (bForce || !stlplus::file_exists(sFeat) || !stlplus::file_exists(sDesc)))
            {
                if (!ReadImage(sView_filename.c_str(), &imageGray))
                    continue;

                //
                // Look if there is occlusion feature mask
                //
                Image<unsigned char> * mask = nullptr; // The mask is null by default

                const std::string
                        mask_filename_local =
                        stlplus::create_filespec(sfm_data.s_root_path,
                                                 stlplus::basename_part(sView_filename) + "_mask", "png"),
                        mask__filename_global =
                        stlplus::create_filespec(sfm_data.s_root_path, "mask", "png");

                Image<unsigned char> imageMask;
                // Try to read the local mask
                if (stlplus::file_exists(mask_filename_local))
                {
                    if (!ReadImage(mask_filename_local.c_str(), &imageMask))
                    {
                        std::cerr << "Invalid mask: " << mask_filename_local << std::endl
                                  << "Stopping feature extraction." << std::endl;
                        preemptive_exit = true;
                        continue;
                    }
                    // Use the local mask only if it fits the current image size
                    if (imageMask.Width() == imageGray.Width() && imageMask.Height() == imageGray.Height())
                        mask = &imageMask;
                }
                else
                {
                    // Try to read the global mask
                    if (stlplus::file_exists(mask__filename_global))
                    {
                        if (!ReadImage(mask__filename_global.c_str(), &imageMask))
                        {
                            std::cerr << "Invalid mask: " << mask__filename_global << std::endl
                                      << "Stopping feature extraction." << std::endl;
                            preemptive_exit = true;
                            continue;
                        }
                        // Use the global mask only if it fits the current image size
                        if (imageMask.Width() == imageGray.Width() && imageMask.Height() == imageGray.Height())
                            mask = &imageMask;
                    }
                }

                // Compute features and descriptors and export them to files
                auto regions = image_describer->Describe(imageGray, mask);
                if (regions && !image_describer->Save(regions.get(), sFeat, sDesc)) {
                    std::cerr << "Cannot save regions for images: " << sView_filename << std::endl
                              << "Stopping feature extraction." << std::endl;
                    preemptive_exit = true;
                    continue;
                }
            }
            ++my_progress_bar;
        }
        std::cout << "Task done in (s): " << timer.elapsed() << std::endl;
    }
    return env->NewStringUTF("SUCCESS");
}