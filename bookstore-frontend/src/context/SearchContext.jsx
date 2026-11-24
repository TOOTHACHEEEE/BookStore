import { createContext, useContext, useState } from 'react'

/**
 * SearchContext - 管理全局搜索状态
 * 
 * 使用 Context API 将搜索关键词提升到应用顶层，
 * 这样 Layout 组件中的搜索框和 BookList 组件中的书籍列表
 * 都可以访问和响应同一个搜索状态。
 */
const SearchContext = createContext()

export function SearchProvider({ children }) {
  // searchKeyword 存储当前的搜索关键词
  const [searchKeyword, setSearchKeyword] = useState('')

  return (
    <SearchContext.Provider value={{ searchKeyword, setSearchKeyword }}>
      {children}
    </SearchContext.Provider>
  )
}

/**
 * 自定义 Hook：方便在组件中使用搜索上下文
 * 使用方式：const { searchKeyword, setSearchKeyword } = useSearch()
 */
export function useSearch() {
  const context = useContext(SearchContext)
  if (!context) {
    throw new Error('useSearch 必须在 SearchProvider 内部使用')
  }
  return context
}
