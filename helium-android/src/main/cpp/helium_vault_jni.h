#ifndef HELIUM_ANDROID_VAULT_JNI_H_
#define HELIUM_ANDROID_VAULT_JNI_H_

#include <jni.h>
#include <string>
#include <vector>

namespace helium {
namespace sync {

// Encapsulates Helium Private Vault E2EE client state
class VaultNativeCore {
 public:
  static VaultNativeCore* GetInstance();

  bool Initialize(const std::string& vault_key, const std::string& storage_path);
  bool IsInitialized() const;
  
  std::string EncryptCommit(const std::string& plaintext_delta);
  std::string DecryptCommit(const std::string& ciphertext_envelope);
  
  bool PushStateDelta(const std::string& entity_type, const std::string& payload);
  std::vector<std::string> PullPendingDeltas(const std::string& entity_type);

 private:
  VaultNativeCore() : is_initialized_(false) {}
  ~VaultNativeCore() = default;

  bool is_initialized_;
  std::string vault_key_;
  std::string storage_path_;
};

}  // namespace sync
}  // namespace helium

#endif  // HELIUM_ANDROID_VAULT_JNI_H_
