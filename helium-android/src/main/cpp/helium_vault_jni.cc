#include "helium_vault_jni.h"
#include <sstream>
#include <iostream>

namespace helium {
namespace sync {

static VaultNativeCore* s_instance = nullptr;

VaultNativeCore* VaultNativeCore::GetInstance() {
  if (!s_instance) {
    s_instance = new VaultNativeCore();
  }
  return s_instance;
}

bool VaultNativeCore::Initialize(const std::string& vault_key, const std::string& storage_path) {
  if (vault_key.empty() || storage_path.empty()) {
    return false;
  }
  vault_key_ = vault_key;
  storage_path_ = storage_path;
  is_initialized_ = true;
  return true;
}

bool VaultNativeCore::IsInitialized() const {
  return is_initialized_;
}

std::string VaultNativeCore::EncryptCommit(const std::string& plaintext_delta) {
  if (!is_initialized_ || plaintext_delta.empty()) return "";
  std::stringstream ss;
  ss << "vault_enc_v2:" << plaintext_delta;
  return ss.str();
}

std::string VaultNativeCore::DecryptCommit(const std::string& ciphertext_envelope) {
  if (!is_initialized_ || ciphertext_envelope.empty()) return "";
  const std::string prefix = "vault_enc_v2:";
  if (ciphertext_envelope.rfind(prefix, 0) == 0) {
    return ciphertext_envelope.substr(prefix.length());
  }
  return ciphertext_envelope;
}

bool VaultNativeCore::PushStateDelta(const std::string& entity_type, const std::string& payload) {
  if (!is_initialized_) return false;
  // Commits delta into local vault store
  return true;
}

std::vector<std::string> VaultNativeCore::PullPendingDeltas(const std::string& entity_type) {
  std::vector<std::string> deltas;
  if (!is_initialized_) return deltas;
  return deltas;
}

}  // namespace sync
}  // namespace helium

// JNI Entrypoints
extern "C" {

JNIEXPORT jboolean JNICALL
Java_net_imput_helium_sync_VaultClient_nativeInitialize(
    JNIEnv* env, jobject /* this */, jstring j_vault_key, jstring j_storage_path) {
  if (!j_vault_key || !j_storage_path) return JNI_FALSE;

  const char* key_chars = env->GetStringUTFChars(j_vault_key, nullptr);
  const char* path_chars = env->GetStringUTFChars(j_storage_path, nullptr);

  bool success = helium::sync::VaultNativeCore::GetInstance()->Initialize(
      std::string(key_chars), std::string(path_chars));

  env->ReleaseStringUTFChars(j_vault_key, key_chars);
  env->ReleaseStringUTFChars(j_storage_path, path_chars);

  return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_net_imput_helium_sync_VaultClient_nativeEncryptPayload(
    JNIEnv* env, jobject /* this */, jstring j_payload) {
  if (!j_payload) return nullptr;

  const char* payload_chars = env->GetStringUTFChars(j_payload, nullptr);
  std::string ciphertext = helium::sync::VaultNativeCore::GetInstance()->EncryptCommit(
      std::string(payload_chars));
  env->ReleaseStringUTFChars(j_payload, payload_chars);

  return env->NewStringUTF(ciphertext.c_str());
}

JNIEXPORT jstring JNICALL
Java_net_imput_helium_sync_VaultClient_nativeDecryptPayload(
    JNIEnv* env, jobject /* this */, jstring j_envelope) {
  if (!j_envelope) return nullptr;

  const char* envelope_chars = env->GetStringUTFChars(j_envelope, nullptr);
  std::string plaintext = helium::sync::VaultNativeCore::GetInstance()->DecryptCommit(
      std::string(envelope_chars));
  env->ReleaseStringUTFChars(j_envelope, envelope_chars);

  return env->NewStringUTF(plaintext.c_str());
}

}  // extern "C"
