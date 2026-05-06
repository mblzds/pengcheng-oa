<template>
  <div class="rich-text-editor" :class="{ focused: isFocused, disabled: disabled }">
    <!-- 工具栏 -->
    <div v-if="!disabled" class="editor-toolbar">
      <div class="toolbar-group">
        <button
          v-for="item in toolbarItems"
          :key="item.action"
          class="toolbar-btn"
          :class="{ active: item.isActive?.() }"
          :title="item.title"
          @click="item.handler"
        >
          <span v-html="item.icon"></span>
        </button>
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <button class="toolbar-btn" title="上传图片" @click="triggerImageUpload">
          <span>🖼</span>
        </button>
      </div>
    </div>

    <!-- 编辑区域 -->
    <div
      ref="editorRef"
      class="editor-content"
      :contenteditable="!disabled"
      :data-placeholder="placeholder"
      @input="handleInput"
      @focus="isFocused = true"
      @blur="handleBlur"
      @paste="handlePaste"
      @keydown="handleKeydown"
    ></div>

    <!-- 底部字数统计 -->
    <div v-if="showWordCount" class="editor-footer">
      <span class="word-count">{{ charCount }} 字</span>
    </div>

    <input ref="imageInputRef" type="file" accept="image/*" style="display: none" @change="handleImageSelect" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  showWordCount?: boolean
  maxLength?: number
}>(), {
  modelValue: '',
  placeholder: '请输入内容...',
  disabled: false,
  showWordCount: true,
  maxLength: 50000
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  uploadImage: [file: File]
}>()

const editorRef = ref<HTMLDivElement>()
const imageInputRef = ref<HTMLInputElement>()
const isFocused = ref(false)

const charCount = computed(() => {
  return editorRef.value?.innerText?.length || 0
})

const toolbarItems = computed(() => [
  {
    action: 'bold',
    title: '粗体 (Ctrl+B)',
    icon: '<b>B</b>',
    handler: () => execCommand('bold'),
    isActive: () => document.queryCommandState('bold')
  },
  {
    action: 'italic',
    title: '斜体 (Ctrl+I)',
    icon: '<i>I</i>',
    handler: () => execCommand('italic'),
    isActive: () => document.queryCommandState('italic')
  },
  {
    action: 'underline',
    title: '下划线 (Ctrl+U)',
    icon: '<u>U</u>',
    handler: () => execCommand('underline'),
    isActive: () => document.queryCommandState('underline')
  },
  {
    action: 'strikethrough',
    title: '删除线',
    icon: '<s>S</s>',
    handler: () => execCommand('strikeThrough'),
    isActive: () => document.queryCommandState('strikeThrough')
  },
  {
    action: 'h2',
    title: '标题',
    icon: 'H',
    handler: () => execCommand('formatBlock', '<h2>'),
    isActive: () => false
  },
  {
    action: 'ul',
    title: '无序列表',
    icon: '•',
    handler: () => execCommand('insertUnorderedList'),
    isActive: () => document.queryCommandState('insertUnorderedList')
  },
  {
    action: 'ol',
    title: '有序列表',
    icon: '1.',
    handler: () => execCommand('insertOrderedList'),
    isActive: () => document.queryCommandState('insertOrderedList')
  },
  {
    action: 'quote',
    title: '引用',
    icon: '"',
    handler: () => execCommand('formatBlock', '<blockquote>'),
    isActive: () => false
  },
  {
    action: 'code',
    title: '代码块',
    icon: '&lt;/&gt;',
    handler: () => execCommand('formatBlock', '<pre>'),
    isActive: () => false
  },
  {
    action: 'link',
    title: '插入链接',
    icon: '🔗',
    handler: insertLink,
    isActive: () => false
  }
])

function execCommand(command: string, value?: string) {
  document.execCommand(command, false, value)
  editorRef.value?.focus()
  emitContent()
}

function insertLink() {
  const url = prompt('请输入链接地址:')
  if (url) {
    execCommand('createLink', url)
  }
}

function handleInput() {
  emitContent()
}

function handleBlur() {
  isFocused.value = false
  emitContent()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Tab') {
    e.preventDefault()
    execCommand('insertHTML', '&nbsp;&nbsp;&nbsp;&nbsp;')
    return
  }

  // Markdown 快捷键（行首输入时自动转换）
  if (e.key === ' ') {
    const sel = window.getSelection()
    if (!sel || sel.rangeCount === 0) return
    const range = sel.getRangeAt(0)
    const node = range.startContainer
    const text = node.textContent || ''
    const offset = range.startOffset

    const lineText = text.substring(0, offset)

    if (lineText === '#') {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('formatBlock', '<h1>')
    } else if (lineText === '##') {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('formatBlock', '<h2>')
    } else if (lineText === '###') {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('formatBlock', '<h3>')
    } else if (lineText === '>') {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('formatBlock', '<blockquote>')
    } else if (lineText === '-' || lineText === '*') {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('insertUnorderedList')
    } else if (/^\d+\.$/.test(lineText)) {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('insertOrderedList')
    } else if (lineText === '```') {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('formatBlock', '<pre>')
    } else if (lineText === '---') {
      e.preventDefault()
      clearLineText(node, offset)
      execCommand('insertHorizontalRule')
    }
  }
}

function clearLineText(node: Node, offset: number) {
  if (node.nodeType === Node.TEXT_NODE) {
    (node as Text).deleteData(0, offset)
  }
}

function handlePaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        emit('uploadImage', file)
      }
      return
    }
  }

  e.preventDefault()
  const text = e.clipboardData?.getData('text/plain') || ''
  execCommand('insertText', text)
}

function triggerImageUpload() {
  imageInputRef.value?.click()
}

function handleImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) {
    emit('uploadImage', input.files[0])
    input.value = ''
  }
}

/**
 * 插入图片到编辑器（外部调用，如图片上传成功后）
 */
function insertImage(url: string) {
  execCommand('insertHTML', `<img src="${url}" style="max-width:100%;" />`)
}

function emitContent() {
  if (editorRef.value) {
    emit('update:modelValue', editorRef.value.innerHTML)
  }
}

onMounted(() => {
  if (editorRef.value && props.modelValue) {
    editorRef.value.innerHTML = props.modelValue
  }
})

watch(() => props.modelValue, (val) => {
  if (editorRef.value && val !== editorRef.value.innerHTML) {
    editorRef.value.innerHTML = val || ''
  }
})

defineExpose({ insertImage })
</script>

<style scoped>
.rich-text-editor {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.rich-text-editor.focused {
  border-color: #18a058;
}
.rich-text-editor.disabled {
  opacity: 0.7;
  background: #f9f9f9;
}
.editor-toolbar {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  flex-wrap: wrap;
  gap: 2px;
}
.toolbar-group {
  display: flex;
  gap: 2px;
}
.toolbar-divider {
  width: 1px;
  height: 20px;
  background: #ddd;
  margin: 0 6px;
}
.toolbar-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
}
.toolbar-btn:hover {
  background: #e8e8e8;
}
.toolbar-btn.active {
  background: #d4edda;
  color: #18a058;
}
.editor-content {
  min-height: 150px;
  max-height: 500px;
  overflow-y: auto;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.7;
  outline: none;
}
.editor-content:empty::before {
  content: attr(data-placeholder);
  color: #ccc;
}
.editor-content :deep(blockquote) {
  border-left: 3px solid #18a058;
  padding-left: 12px;
  margin: 8px 0;
  color: #666;
}
.editor-content :deep(pre) {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-family: monospace;
  overflow-x: auto;
}
.editor-content :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}
.editor-content :deep(a) {
  color: #18a058;
  text-decoration: underline;
}
.editor-footer {
  display: flex;
  justify-content: flex-end;
  padding: 4px 12px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}
.word-count {
  font-size: 12px;
  color: #999;
}
</style>
