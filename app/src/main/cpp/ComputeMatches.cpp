#include "openMVG/graph/graph.hpp"
#include "openMVG/graph/graph_stats.hpp"
#include "openMVG/features/akaze/image_describer_akaze.hpp"
#include "openMVG/features/descriptor.hpp"
#include "openMVG/features/feature.hpp"
#include "openMVG/matching/indMatch.hpp"
#include "openMVG/matching/indMatch_utils.hpp"
#include "openMVG/matching_image_collection/Matcher_Regions.hpp"
#include "openMVG/matching_image_collection/Cascade_Hashing_Matcher_Regions.hpp"
#include "openMVG/matching_image_collection/GeometricFilter.hpp"
#include "openMVG/sfm/pipelines/sfm_features_provider.hpp"
#include "openMVG/sfm/pipelines/sfm_regions_provider.hpp"
#include "openMVG/sfm/pipelines/sfm_regions_provider_cache.hpp"
#include "openMVG/matching_image_collection/F_ACRobust.hpp"
#include "openMVG/matching_image_collection/E_ACRobust.hpp"
#include "openMVG/matching_image_collection/E_ACRobust_Angular.hpp"
#include "openMVG/matching_image_collection/Eo_Robust.hpp"
#include "openMVG/matching_image_collection/H_ACRobust.hpp"
#include "openMVG/matching_image_collection/Pair_Builder.hpp"
#include "openMVG/matching/pairwiseAdjacencyDisplay.hpp"
#include "openMVG/sfm/sfm_data.hpp"
#include "openMVG/sfm/sfm_data_io.hpp"
#include "openMVG/stl/stl.hpp"
#include "openMVG/system/timer.hpp"

#include "third_party/stlplus3/filesystemSimplified/file_system.hpp"

#include <cstdlib>
#include <iostream>
#include <memory>
#include <string>

#include <jni.h>

using namespace openMVG;
using namespace openMVG::matching;
using namespace openMVG::robust;
using namespace openMVG::sfm;
using namespace openMVG::matching_image_collection;
using namespace std;

enum EGeometricModel
{
    FUNDAMENTAL_MATRIX = 0,
    ESSENTIAL_MATRIX   = 1,
    HOMOGRAPHY_MATRIX  = 2,
    ESSENTIAL_MATRIX_ANGULAR = 3,
    ESSENTIAL_MATRIX_ORTHO = 4,
    ESSENTIAL_MATRIX_UPRIGHT = 5
};

enum EPairMode
{
    PAIR_EXHAUSTIVE = 0,
    PAIR_CONTIGUOUS = 1,
    PAIR_FROM_FILE  = 2
};

extern "C" JNIEXPORT jstring JNICALL
Java_ir_sami_trowel_services_BuildModelTask_computeMatches(
        JNIEnv *env,
        jobject /* this */, jstring jsSfM_Data_Filename, jstring jsMatchesDirectory, jstring jsGeometricModel, jstring jfDistRatio, jstring jsNearestMatchingMethod) {


    jboolean copy = true;
    std::string sSfM_Data_Filename = env->GetStringUTFChars(jsSfM_Data_Filename, &copy);
    std::string sMatchesDirectory = env->GetStringUTFChars(jsMatchesDirectory, &copy);
    std::string sGeometricModel = env->GetStringUTFChars(jsGeometricModel, &copy);
    float fDistRatio = std::stof(env->GetStringUTFChars(jfDistRatio, &copy));
    int iMatchingVideoMode = -1;
    std::string sPredefinedPairList = "";
    std::string sNearestMatchingMethod = env->GetStringUTFChars(jsNearestMatchingMethod, &copy);
    bool bForce = false;
    bool bGuided_matching = false;
    int imax_iteration = 2048;
    unsigned int ui_max_cache_size = 0;

    EPairMode ePairmode = (iMatchingVideoMode == -1 ) ? PAIR_EXHAUSTIVE : PAIR_CONTIGUOUS;

    if (sPredefinedPairList.length()) {
        ePairmode = PAIR_FROM_FILE;
        if (iMatchingVideoMode>0) {
            std::cerr << "\nIncompatible options: --videoModeMatching and --pairList" << std::endl;
            return env->NewStringUTF("FAILED");
        }
    }

    if (sMatchesDirectory.empty() || !stlplus::is_folder(sMatchesDirectory))  {
        std::cerr << "\nIt is an invalid output directory" << std::endl;
        return env->NewStringUTF("FAILED");
    }

    EGeometricModel eGeometricModelToCompute = FUNDAMENTAL_MATRIX;
    std::string sGeometricMatchesFilename = "";
    switch (sGeometricModel[0])
    {
        case 'f': case 'F':
            eGeometricModelToCompute = FUNDAMENTAL_MATRIX;
            sGeometricMatchesFilename = "matches.f.bin";
            break;
        case 'e': case 'E':
            eGeometricModelToCompute = ESSENTIAL_MATRIX;
            sGeometricMatchesFilename = "matches.e.bin";
            break;
        case 'h': case 'H':
            eGeometricModelToCompute = HOMOGRAPHY_MATRIX;
            sGeometricMatchesFilename = "matches.h.bin";
            break;
        case 'a': case 'A':
            eGeometricModelToCompute = ESSENTIAL_MATRIX_ANGULAR;
            sGeometricMatchesFilename = "matches.f.bin";
            break;
        case 'o': case 'O':
            eGeometricModelToCompute = ESSENTIAL_MATRIX_ORTHO;
            sGeometricMatchesFilename = "matches.o.bin";
            break;
        case 'u': case 'U':
            eGeometricModelToCompute = ESSENTIAL_MATRIX_UPRIGHT;
            sGeometricMatchesFilename = "matches.f.bin";
            break;
        default:
            std::cerr << "Unknown geometric model" << std::endl;
            return env->NewStringUTF("FAILED");
    }

    // -----------------------------
    // - Load SfM_Data Views & intrinsics data
    // a. Compute putative descriptor matches
    // b. Geometric filtering of putative matches
    // + Export some statistics
    // -----------------------------

    //---------------------------------------
    // Read SfM Scene (image view & intrinsics data)
    //---------------------------------------
    SfM_Data sfm_data;
    if (!Load(sfm_data, sSfM_Data_Filename, ESfM_Data(VIEWS|INTRINSICS))) {
        std::cerr << std::endl
                  << "The input SfM_Data file \""<< sSfM_Data_Filename << "\" cannot be read." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    //---------------------------------------
    // Load SfM Scene regions
    //---------------------------------------
    // Init the regions_type from the image describer file (used for image regions extraction)
    using namespace openMVG::features;
    const std::string sImage_describer = stlplus::create_filespec(sMatchesDirectory, "image_describer", "json");
    std::unique_ptr<Regions> regions_type = Init_region_type_from_file(sImage_describer);
    if (!regions_type)
    {
        std::cerr << "Invalid: "
                  << sImage_describer << " regions type file." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    //---------------------------------------
    // a. Compute putative descriptor matches
    //    - Descriptor matching (according user method choice)
    //    - Keep correspondences only if NearestNeighbor ratio is ok
    //---------------------------------------

    // Load the corresponding view regions
    std::shared_ptr<Regions_Provider> regions_provider;
    if (ui_max_cache_size == 0)
    {
        // Default regions provider (load & store all regions in memory)
        regions_provider = std::make_shared<Regions_Provider>();
    }
    else
    {
        // Cached regions provider (load & store regions on demand)
        regions_provider = std::make_shared<Regions_Provider_Cache>(ui_max_cache_size);
    }

    // Show the progress on the command line:
    C_Progress_display progress;

    if (!regions_provider->load(sfm_data, sMatchesDirectory, regions_type, &progress)) {
        std::cerr << std::endl << "Invalid regions." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    PairWiseMatches map_PutativesMatches;

    // Build some alias from SfM_Data Views data:
    // - List views as a vector of filenames & image sizes
    std::vector<std::string> vec_fileNames;
    std::vector<std::pair<size_t, size_t>> vec_imagesSize;
    {
        vec_fileNames.reserve(sfm_data.GetViews().size());
        vec_imagesSize.reserve(sfm_data.GetViews().size());
        for (Views::const_iterator iter = sfm_data.GetViews().begin();
             iter != sfm_data.GetViews().end();
             ++iter)
        {
            const View * v = iter->second.get();
            vec_fileNames.push_back(stlplus::create_filespec(sfm_data.s_root_path,
                                                             v->s_Img_path));
            vec_imagesSize.push_back( std::make_pair( v->ui_width, v->ui_height) );
        }
    }

    std::cout << std::endl << " - PUTATIVE MATCHES - " << std::endl;
    // If the matches already exists, reload them
    if (!bForce
        && (stlplus::file_exists(sMatchesDirectory + "/matches.putative.txt")
            || stlplus::file_exists(sMatchesDirectory + "/matches.putative.bin"))
            )
    {
        if (!(Load(map_PutativesMatches, sMatchesDirectory + "/matches.putative.bin") ||
              Load(map_PutativesMatches, sMatchesDirectory + "/matches.putative.txt")) )
        {
            std::cerr << "Cannot load input matches file";
            return env->NewStringUTF("FAILED");
        }
        std::cout << "\t PREVIOUS RESULTS LOADED;"
                  << " #pair: " << map_PutativesMatches.size() << std::endl;
    }
    else // Compute the putative matches
    {
        std::cout << "Use: ";
        switch (ePairmode)
        {
            case PAIR_EXHAUSTIVE: std::cout << "exhaustive pairwise matching" << std::endl; break;
            case PAIR_CONTIGUOUS: std::cout << "sequence pairwise matching" << std::endl; break;
            case PAIR_FROM_FILE:  std::cout << "user defined pairwise matching" << std::endl; break;
        }

        // Allocate the right Matcher according the Matching requested method
        std::unique_ptr<Matcher> collectionMatcher;
        if (sNearestMatchingMethod == "AUTO")
        {
            if (regions_type->IsScalar())
            {
                std::cout << "Using FAST_CASCADE_HASHING_L2 matcher" << std::endl;
                collectionMatcher.reset(new Cascade_Hashing_Matcher_Regions(fDistRatio));
            }
            else
            if (regions_type->IsBinary())
            {
                std::cout << "Using BRUTE_FORCE_HAMMING matcher" << std::endl;
                collectionMatcher.reset(new Matcher_Regions(fDistRatio, BRUTE_FORCE_HAMMING));
            }
        }
        else
        if (sNearestMatchingMethod == "BRUTEFORCEL2")
        {
            std::cout << "Using BRUTE_FORCE_L2 matcher" << std::endl;
            collectionMatcher.reset(new Matcher_Regions(fDistRatio, BRUTE_FORCE_L2));
        }
        else
        if (sNearestMatchingMethod == "BRUTEFORCEHAMMING")
        {
            std::cout << "Using BRUTE_FORCE_HAMMING matcher" << std::endl;
            collectionMatcher.reset(new Matcher_Regions(fDistRatio, BRUTE_FORCE_HAMMING));
        }
        else
        if (sNearestMatchingMethod == "HNSWL2")
        {
            std::cout << "Using HNSWL2 matcher" << std::endl;
            collectionMatcher.reset(new Matcher_Regions(fDistRatio, HNSW_L2));
        }
        else
        if (sNearestMatchingMethod == "ANNL2")
        {
            std::cout << "Using ANN_L2 matcher" << std::endl;
            collectionMatcher.reset(new Matcher_Regions(fDistRatio, ANN_L2));
        }
        else
        if (sNearestMatchingMethod == "CASCADEHASHINGL2")
        {
            std::cout << "Using CASCADE_HASHING_L2 matcher" << std::endl;
            collectionMatcher.reset(new Matcher_Regions(fDistRatio, CASCADE_HASHING_L2));
        }
        else
        if (sNearestMatchingMethod == "FASTCASCADEHASHINGL2")
        {
            std::cout << "Using FAST_CASCADE_HASHING_L2 matcher" << std::endl;
            collectionMatcher.reset(new Cascade_Hashing_Matcher_Regions(fDistRatio));
        }
        if (!collectionMatcher)
        {
            std::cerr << "Invalid Nearest Neighbor method: " << sNearestMatchingMethod << std::endl;
            return env->NewStringUTF("FAILED");
        }
        // Perform the matching
        system::Timer timer;
        {
            // From matching mode compute the pair list that have to be matched:
            Pair_Set pairs;
            switch (ePairmode)
            {
                case PAIR_EXHAUSTIVE: pairs = exhaustivePairs(sfm_data.GetViews().size()); break;
                case PAIR_CONTIGUOUS: pairs = contiguousWithOverlap(sfm_data.GetViews().size(), iMatchingVideoMode); break;
                case PAIR_FROM_FILE:
                    if (!loadPairs(sfm_data.GetViews().size(), sPredefinedPairList, pairs))
                    {
                        return env->NewStringUTF("FAILED");
                    }
                    break;
            }
            // Photometric matching of putative pairs
            collectionMatcher->Match(regions_provider, pairs, map_PutativesMatches, &progress);
            //---------------------------------------
            //-- Export putative matches
            //---------------------------------------
            if (!Save(map_PutativesMatches, std::string(sMatchesDirectory + "/matches.putative.bin")))
            {
                std::cerr
                        << "Cannot save computed matches in: "
                        << std::string(sMatchesDirectory + "/matches.putative.bin");
                return env->NewStringUTF("FAILED");
            }
        }
        std::cout << "Task (Regions Matching) done in (s): " << timer.elapsed() << std::endl;
    }
    //-- export putative matches Adjacency matrix
    PairWiseMatchingToAdjacencyMatrixSVG(vec_fileNames.size(),
                                         map_PutativesMatches,
                                         stlplus::create_filespec(sMatchesDirectory, "PutativeAdjacencyMatrix", "svg"));
    //-- export view pair graph once putative graph matches have been computed
    {
        std::set<IndexT> set_ViewIds;
        std::transform(sfm_data.GetViews().begin(), sfm_data.GetViews().end(),
                       std::inserter(set_ViewIds, set_ViewIds.begin()), stl::RetrieveKey());
        graph::indexedGraph putativeGraph(set_ViewIds, getPairs(map_PutativesMatches));
        graph::exportToGraphvizData(
                stlplus::create_filespec(sMatchesDirectory, "putative_matches"),
                putativeGraph);
    }

    //---------------------------------------
    // b. Geometric filtering of putative matches
    //    - AContrario Estimation of the desired geometric model
    //    - Use an upper bound for the a contrario estimated threshold
    //---------------------------------------

    std::unique_ptr<ImageCollectionGeometricFilter> filter_ptr(
            new ImageCollectionGeometricFilter(&sfm_data, regions_provider));

    if (filter_ptr)
    {
        system::Timer timer;
        const double d_distance_ratio = 0.6;

        PairWiseMatches map_GeometricMatches;
        switch (eGeometricModelToCompute)
        {
            case HOMOGRAPHY_MATRIX:
            {
                const bool bGeometric_only_guided_matching = true;
                filter_ptr->Robust_model_estimation(
                        GeometricFilter_HMatrix_AC(4.0, imax_iteration),
                        map_PutativesMatches, bGuided_matching,
                        bGeometric_only_guided_matching ? -1.0 : d_distance_ratio, &progress);
                map_GeometricMatches = filter_ptr->Get_geometric_matches();
            }
                break;
            case FUNDAMENTAL_MATRIX:
            {
                filter_ptr->Robust_model_estimation(
                        GeometricFilter_FMatrix_AC(4.0, imax_iteration),
                        map_PutativesMatches, bGuided_matching, d_distance_ratio, &progress);
                map_GeometricMatches = filter_ptr->Get_geometric_matches();
            }
                break;
            case ESSENTIAL_MATRIX:
            {
                filter_ptr->Robust_model_estimation(
                        GeometricFilter_EMatrix_AC(4.0, imax_iteration),
                        map_PutativesMatches, bGuided_matching, d_distance_ratio, &progress);
                map_GeometricMatches = filter_ptr->Get_geometric_matches();

                //-- Perform an additional check to remove pairs with poor overlap
                std::vector<PairWiseMatches::key_type> vec_toRemove;
                for (const auto & pairwisematches_it : map_GeometricMatches)
                {
                    const size_t putativePhotometricCount = map_PutativesMatches.find(pairwisematches_it.first)->second.size();
                    const size_t putativeGeometricCount = pairwisematches_it.second.size();
                    const float ratio = putativeGeometricCount / static_cast<float>(putativePhotometricCount);
                    if (putativeGeometricCount < 50 || ratio < .3f)  {
                        // the pair will be removed
                        vec_toRemove.push_back(pairwisematches_it.first);
                    }
                }
                //-- remove discarded pairs
                for (const auto & pair_to_remove_it : vec_toRemove)
                {
                    map_GeometricMatches.erase(pair_to_remove_it);
                }
            }
                break;
            case ESSENTIAL_MATRIX_ANGULAR:
            {
                filter_ptr->Robust_model_estimation(
                        GeometricFilter_ESphericalMatrix_AC_Angular<false>(4.0, imax_iteration),
                        map_PutativesMatches, bGuided_matching, d_distance_ratio, &progress);
                map_GeometricMatches = filter_ptr->Get_geometric_matches();
            }
                break;
            case ESSENTIAL_MATRIX_ORTHO:
            {
                filter_ptr->Robust_model_estimation(
                        GeometricFilter_EOMatrix_RA(2.0, imax_iteration),
                        map_PutativesMatches, bGuided_matching, d_distance_ratio, &progress);
                map_GeometricMatches = filter_ptr->Get_geometric_matches();
            }
                break;
            case ESSENTIAL_MATRIX_UPRIGHT:
            {
                filter_ptr->Robust_model_estimation(
                        GeometricFilter_ESphericalMatrix_AC_Angular<true>(4.0, imax_iteration),
                        map_PutativesMatches, bGuided_matching, d_distance_ratio, &progress);
                map_GeometricMatches = filter_ptr->Get_geometric_matches();
            }
                break;
        }

        //---------------------------------------
        //-- Export geometric filtered matches
        //---------------------------------------
        if (!Save(map_GeometricMatches,
                  std::string(sMatchesDirectory + "/" + sGeometricMatchesFilename)))
        {
            std::cerr
                    << "Cannot save computed matches in: "
                    << std::string(sMatchesDirectory + "/" + sGeometricMatchesFilename);
            return env->NewStringUTF("FAILED");
        }

        std::cout << "Task done in (s): " << timer.elapsed() << std::endl;

        // -- export Geometric View Graph statistics
        graph::getGraphStatistics(sfm_data.GetViews().size(), getPairs(map_GeometricMatches));

        //-- export Adjacency matrix
        std::cout << "\n Export Adjacency Matrix of the pairwise's geometric matches"
                  << std::endl;
        PairWiseMatchingToAdjacencyMatrixSVG(vec_fileNames.size(),
                                             map_GeometricMatches,
                                             stlplus::create_filespec(sMatchesDirectory, "GeometricAdjacencyMatrix", "svg"));

        //-- export view pair graph once geometric filter have been done
        {
            std::set<IndexT> set_ViewIds;
            std::transform(sfm_data.GetViews().begin(), sfm_data.GetViews().end(),
                           std::inserter(set_ViewIds, set_ViewIds.begin()), stl::RetrieveKey());
            graph::indexedGraph putativeGraph(set_ViewIds, getPairs(map_GeometricMatches));
            graph::exportToGraphvizData(
                    stlplus::create_filespec(sMatchesDirectory, "geometric_matches"),
                    putativeGraph);
        }
    }
    return env->NewStringUTF("SUCCESS");
}
