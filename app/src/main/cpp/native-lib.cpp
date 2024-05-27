#include <jni.h>
#include <string>
#include <sstream>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#include <opencv2/video.hpp>
#include "android/bitmap.h"

using namespace std;
using namespace cv;

void bitmapToMat(JNIEnv * env, jobject bitmap, cv::Mat &dst, jboolean needUnPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void* pixels = 0;
    try {
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        dst.create(info.height, info.width, CV_8UC4);
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(needUnPremultiplyAlpha) cvtColor(tmp, dst, cv::COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, cv::COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}

void matToBitmap(JNIEnv * env, cv::Mat src, jobject bitmap, jboolean needPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void* pixels = 0;
    try {
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( src.dims == 2 && info.height == (uint32_t)src.rows && info.width == (uint32_t)src.cols );
        CV_Assert( src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(src.type() == CV_8UC1) {
                cvtColor(src, tmp, cv::COLOR_GRAY2RGBA);
            } else if(src.type() == CV_8UC3) {
                cvtColor(src, tmp, cv::COLOR_RGB2RGBA);
            } else if(src.type() == CV_8UC4) {
                if(needPremultiplyAlpha) cvtColor(src, tmp, cv::COLOR_RGBA2mRGBA);
                else src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if(src.type() == CV_8UC1) {
                cvtColor(src, tmp, cv::COLOR_GRAY2BGR565);
            } else if(src.type() == CV_8UC3) {
                cvtColor(src, tmp, cv::COLOR_RGB2BGR565);
            } else if(src.type() == CV_8UC4) {
                cvtColor(src, tmp, cv::COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_aplicacionnativa_MainActivity_fuego(
        JNIEnv* env,
        jobject /* this */ ,
        jobject fotoObj ,
        jobject imagenObj,
        jobject resultadoObj) {
// Convertir los objetos Bitmap de entrada a matrices Mat de OpenCV
    Mat fotoMat, imagenMat;
    bitmapToMat(env, fotoObj, fotoMat, false);
    bitmapToMat(env, imagenObj, imagenMat, false);

    // Asegurarse de que las imágenes tengan el mismo tamaño
    resize(imagenMat, imagenMat, fotoMat.size());

    // Crear una imagen para almacenar el resultado
    Mat imagenCombinada = fotoMat.clone();

    // Definir un umbral de tolerancia para considerar un píxel como blanco o cercano a blanco
    int umbralBlanco = 200;

    // Recorrer los píxeles de la imagen y aplicar los valores de la imagen de agua a los píxeles blancos o cercanos a blanco
    for (int i = 0; i < fotoMat.rows; ++i) {
        for (int j = 0; j < fotoMat.cols; ++j) {
            Vec3b colorFoto = fotoMat.at<Vec3b>(i, j);
            if (colorFoto[0] > umbralBlanco && colorFoto[1] > umbralBlanco && colorFoto[2] > umbralBlanco) {
                // Aplicar los valores de la imagen de agua
                Vec3b colorImagen = imagenMat.at<Vec3b>(i, j);
                imagenCombinada.at<Vec3b>(i, j) = colorImagen;
            }
        }
    }

    // Convertir la imagen combinada de Mat a Bitmap y asignarla al objeto Bitmap resultado
    matToBitmap(env, imagenCombinada, resultadoObj, false);


}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_aplicacionnativa_MainActivity_quitarFondo(
        JNIEnv* env,
        jobject /* this */,
        jobject fotoObj,
        jobject resultadoObj){

    // Convertir los objetos Bitmap de entrada a matrices Mat de OpenCV
    Mat fotoMat, resultadoMat;
    bitmapToMat(env, fotoObj, fotoMat, false);

    // Convertir la imagen de entrada a espacio de color LAB
    Mat labMat;
    cvtColor(fotoMat, labMat, COLOR_BGR2Lab);

    // Extraer el canal A
    vector<Mat> lab_planes;
    split(labMat, lab_planes);
    Mat A = lab_planes[1];

    // Aplicar el umbral al canal A usando el método de Otsu
    Mat thresh;
    threshold(A, thresh, 0, 255, THRESH_BINARY + THRESH_OTSU);

    // Desenfocar la imagen umbralizada
    Mat blur;
    GaussianBlur(thresh, blur, Size(0, 0), 5, 5, BORDER_DEFAULT);

    // Estirar la intensidad de la imagen desenfocada para crear una máscara
    Mat mask;
    normalize(blur, mask, 0, 255, NORM_MINMAX, CV_8UC1);

    // Añadir la máscara como un canal alfa a la imagen original
    Mat result;
    cvtColor(fotoMat, result, COLOR_BGR2BGRA);
    vector<Mat> rgba_planes;
    split(result, rgba_planes);
    rgba_planes[3] = mask;
    merge(rgba_planes, result);

    // Convertir la imagen resultante de Mat a Bitmap y asignarla al objeto Bitmap resultado
    matToBitmap(env, result, resultadoObj, false);
}



extern "C"
JNIEXPORT void JNICALL
Java_com_example_aplicacionnativa_MainActivity_bordes(
        JNIEnv* env,
        jobject /* this */,
        jobject fotoObj,
        jobject resultadoObj,
        jint umbral){

    // Convertir los objetos Bitmap de entrada a matrices Mat de OpenCV
    Mat fotoMat, resultadoMat;
    bitmapToMat(env, fotoObj, fotoMat, false);

    // Convertir la imagen de entrada a espacio de color LAB
    Mat labMat;
    cvtColor(fotoMat, labMat, COLOR_BGR2Lab);

    // Extraer el canal A
    vector<Mat> lab_planes;
    split(labMat, lab_planes);
    Mat A = lab_planes[1];

    // Aplicar la binarización por umbral de color en el canal A
    Mat binarizada;
    threshold(A, binarizada, umbral, 255, THRESH_BINARY);

    // Convertir el resultado binarizado a Bitmap y asignarlo al objeto resultado
    matToBitmap(env, binarizada, resultadoObj, false);
}
